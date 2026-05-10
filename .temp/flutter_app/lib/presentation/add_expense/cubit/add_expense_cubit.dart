import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import 'package:rxdart/rxdart.dart';
import '../../../domain/models/expense.dart';
import '../../../domain/repository/expense_repository.dart';
import '../../../domain/repository/installment_repository.dart';
import '../../../domain/use_cases/add_expense_use_case.dart';
import '../../../domain/use_cases/create_installment_plan_use_case.dart';
import '../../../domain/use_cases/get_categories_use_case.dart';
import '../../../domain/use_cases/get_expense_by_id_use_case.dart';
import '../../../domain/use_cases/get_item_name_suggestions_use_case.dart';
import '../../../domain/use_cases/get_merchant_suggestions_use_case.dart';
import '../../../domain/use_cases/update_expense_use_case.dart';
import 'add_expense_state.dart';

@injectable
class AddExpenseCubit extends Cubit<AddExpenseState> {
  final AddExpenseUseCase _addExpenseUseCase;
  final UpdateExpenseUseCase _updateExpenseUseCase;
  final GetExpenseByIdUseCase _getExpenseByIdUseCase;
  final GetCategoriesUseCase _getCategoriesUseCase;
  final CreateInstallmentPlanUseCase _createInstallmentPlanUseCase;
  final GetItemNameSuggestionsUseCase _getItemNameSuggestionsUseCase;
  final GetMerchantSuggestionsUseCase _getMerchantSuggestionsUseCase;
  final InstallmentRepository _installmentRepository;
  final ExpenseRepository _expenseRepository;

  final _itemNameSubject = PublishSubject<String>();
  final _merchantSubject = PublishSubject<String>();

  int? _editExpenseId;

  AddExpenseCubit(
    this._addExpenseUseCase,
    this._updateExpenseUseCase,
    this._getExpenseByIdUseCase,
    this._getCategoriesUseCase,
    this._createInstallmentPlanUseCase,
    this._getItemNameSuggestionsUseCase,
    this._getMerchantSuggestionsUseCase,
    this._installmentRepository,
    this._expenseRepository,
  ) : super(AddExpenseState(selectedDate: DateTime.now().millisecondsSinceEpoch)) {
    _init();
  }

  void _init() {
    _getCategoriesUseCase().listen((cats) {
      emit(state.copyWith(categories: cats));
    });

    _expenseRepository.getAllTags().listen((tags) {
      emit(state.copyWith(allTags: tags));
    });

    _itemNameSubject
        .debounceTime(const Duration(milliseconds: 300))
        .distinct()
        .listen((query) async {
      if (query.length >= 2) {
        final suggestions = await _getItemNameSuggestionsUseCase(query);
        emit(state.copyWith(itemNameSuggestions: suggestions));
      } else {
        emit(state.copyWith(itemNameSuggestions: []));
      }
    });

    _merchantSubject
        .debounceTime(const Duration(milliseconds: 300))
        .distinct()
        .listen((query) async {
      if (query.length >= 2) {
        final suggestions = await _getMerchantSuggestionsUseCase(query);
        emit(state.copyWith(merchantSuggestions: suggestions));
      } else {
        emit(state.copyWith(merchantSuggestions: []));
      }
    });
  }

  void loadExpense(int id) async {
    _editExpenseId = id;
    final expense = await _getExpenseByIdUseCase(id);
    if (expense != null) {
      emit(state.copyWith(
        amount: (expense.amount / 100.0).toStringAsFixed(2),
        itemName: expense.itemName,
        merchant: expense.merchant ?? "",
        categoryId: expense.categoryId,
        isInstallment: expense.isInstallment,
        selectedDate: expense.date,
        selectedTags: expense.tags,
      ));
      
      if (expense.isInstallment) {
        final plan = await _installmentRepository.getInstallmentByExpenseId(id);
        if (plan != null) {
          emit(state.copyWith(durationMonths: plan.durationMonths.toString()));
        }
      }
    }
  }

  void onAmountChanged(String value) => emit(state.copyWith(amount: value));
  void onItemNameChanged(String value) {
    emit(state.copyWith(itemName: value));
    _itemNameSubject.add(value);
  }
  void onMerchantChanged(String value) {
    emit(state.copyWith(merchant: value));
    _merchantSubject.add(value);
  }
  void onCategoryChanged(int id) => emit(state.copyWith(categoryId: id));
  void onDateChanged(int timestamp) => emit(state.copyWith(selectedDate: timestamp));
  void toggleInstallment(bool value) => emit(state.copyWith(isInstallment: value));
  void onDurationChanged(String value) => emit(state.copyWith(durationMonths: value));

  void toggleTag(String tag) {
    final newTags = List<String>.from(state.selectedTags);
    if (newTags.contains(tag)) {
      newTags.remove(tag);
    } else {
      newTags.add(tag);
    }
    emit(state.copyWith(selectedTags: newTags));
  }

  void save() async {
    emit(state.copyWith(isLoading: true));
    
    final amountInCents = _toSafeLongCents(state.amount);
    
    final expense = Expense(
      id: _editExpenseId ?? 0,
      date: state.selectedDate,
      itemName: state.itemName.isEmpty ? "Uncategorized Item" : state.itemName,
      amount: amountInCents,
      categoryId: state.categoryId,
      isInstallment: state.isInstallment,
      merchant: state.merchant.isEmpty ? null : state.merchant,
      tags: state.selectedTags,
      quantity: 1,
    );

    try {
      if (_editExpenseId == null) {
        final id = await _addExpenseUseCase(expense);
        if (state.isInstallment && state.durationMonths.isNotEmpty) {
          final duration = int.tryParse(state.durationMonths) ?? 0;
          await _createInstallmentPlanUseCase(
            expenseId: id,
            totalAmount: amountInCents,
            durationMonths: duration,
            startDate: state.selectedDate,
          );
        }
      } else {
        await _updateExpenseUseCase(expense, durationMonths: int.tryParse(state.durationMonths));
      }
      emit(state.copyWith(isLoading: false, isSaved: true));
    } catch (e) {
      emit(state.copyWith(isLoading: false, error: e.toString()));
    }
  }

  int _toSafeLongCents(String value) {
    final cleanStr = value.replaceAll(',', '.');
    final pieces = cleanStr.split('.');
    final major = int.tryParse(pieces[0]) ?? 0;
    int minor = 0;
    if (pieces.length > 1) {
      final m = pieces[1].padRight(2, '0').substring(0, 2);
      minor = int.tryParse(m) ?? 0;
    }
    return (major * 100) + minor;
  }

  @override
  Future<void> close() {
    _itemNameSubject.close();
    _merchantSubject.close();
    return super.close();
  }
}
