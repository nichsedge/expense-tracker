import 'dart:async';
import 'dart:io';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import 'package:csv/csv.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:intl/intl.dart';
import '../../../domain/repository/expense_repository.dart';
import '../../../domain/preferences/budget_preferences.dart';
import '../../../domain/models/category.dart';
import '../../../domain/models/tag.dart';
import '../../../core/util/csv_parser.dart';
import 'settings_state.dart';

@injectable
class SettingsCubit extends Cubit<SettingsState> {
  final ExpenseRepository _repository;
  final BudgetPreferences _budgetPreferences;

  StreamSubscription? _categoriesSubscription;
  StreamSubscription? _tagsSubscription;
  StreamSubscription? _budgetSubscription;

  SettingsCubit(this._repository, this._budgetPreferences) : super(const SettingsState()) {
    _init();
  }

  void _init() {
    _categoriesSubscription = _repository.getAllCategories().listen((categories) {
      emit(state.copyWith(categories: categories));
    });

    _tagsSubscription = _repository.getAllTagEntities().listen((tags) {
      emit(state.copyWith(tags: tags));
    });

    _budgetSubscription = _budgetPreferences.getMonthlyBudget().listen((budget) {
      emit(state.copyWith(monthlyBudget: budget));
    });
  }

  Future<void> updateMonthlyBudget(int amount) async {
    await _budgetPreferences.setMonthlyBudget(amount);
  }

  Future<void> addCategory(String name, String icon) async {
    await _repository.addCategory(Category(id: 0, name: name, icon: icon, orderIndex: state.categories.length));
  }

  Future<void> updateCategory(Category category) async {
    await _repository.updateCategory(category);
  }

  Future<void> deleteCategory(Category category) async {
    await _repository.deleteCategory(category);
  }

  Future<void> reorderCategories(List<Category> reordered) async {
    final updated = reordered.asMap().entries.map((e) => e.value.copyWith(orderIndex: e.key)).toList();
    await _repository.updateCategories(updated);
  }

  Future<void> updateTag(Tag tag) async {
    await _repository.updateTag(tag);
  }

  Future<void> deleteTag(Tag tag) async {
    await _repository.deleteTag(tag);
  }

  Future<void> reorderTags(List<Tag> reordered) async {
    final updated = reordered.asMap().entries.map((e) => e.value.copyWith(orderIndex: e.key)).toList();
    await _repository.updateTags(updated);
  }

  Future<void> exportToCsv() async {
    emit(state.copyWith(isLoading: true));
    try {
      final expenses = await _repository.getAllExpensesFuture();
      final header = ['Date', 'Platform', 'Merchant', 'Item Name', 'Quantity', 'Price', 'Total', 'Status', 'Installment'];
      final rows = [
        header,
        ...expenses.map((e) => [
          DateFormat('yyyy-MM-dd').format(DateTime.fromMillisecondsSinceEpoch(e.date)),
          '',
          e.merchant ?? '',
          e.itemName,
          e.quantity,
          e.amount / 100,
          (e.amount * e.quantity) / 100,
          'Completed',
          e.isInstallment ? '1' : '0',
        ]),
      ];

      final csvContent = ListToCsvConverter().convert(rows);
      final directory = await getTemporaryDirectory();
      final file = File('${directory.path}/expenses_export.csv');
      await file.writeAsString(csvContent);

      await Share.shareXFiles([XFile(file.path)], text: 'My Expenses Export');
      emit(state.copyWith(isLoading: false, successMessage: 'Export successful!'));
    } catch (e) {
      emit(state.copyWith(isLoading: false, error: 'Export failed: $e'));
    }
  }

  Future<void> importFromCsv(String filePath) async {
    emit(state.copyWith(isLoading: true));
    try {
      final file = File(filePath);
      final content = await file.readAsString();
      final expenses = CsvParser.parse(content);
      
      if (expenses.isEmpty) {
        emit(state.copyWith(isLoading: false, error: 'No valid expenses found in CSV'));
        return;
      }

      await _repository.addExpenses(expenses);
      emit(state.copyWith(isLoading: false, successMessage: 'Imported ${expenses.length} expenses!'));
    } catch (e) {
      emit(state.copyWith(isLoading: false, error: 'Import failed: $e'));
    }
  }

  void clearMessages() {
    emit(state.copyWith(error: null, successMessage: null));
  }

  @override
  Future<void> close() {
    _categoriesSubscription?.cancel();
    _tagsSubscription?.cancel();
    _budgetSubscription?.cancel();
    return super.close();
  }
}
