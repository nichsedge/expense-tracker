import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import 'package:rxdart/rxdart.dart';
import '../../../core/util/date_formatter_utils.dart';
import '../../../core/util/currency_formatter.dart';
import '../../../domain/models/expense.dart';
import '../../../domain/preferences/budget_preferences.dart';
import '../../../domain/repository/expense_repository.dart';
import '../../../domain/repository/installment_repository.dart';
import '../../../domain/use_cases/get_categories_use_case.dart';
import 'expense_list_state.dart';

@injectable
class ExpenseListCubit extends Cubit<ExpenseListState> {
  final ExpenseRepository _repository;
  final InstallmentRepository _installmentRepository;
  final GetCategoriesUseCase _getCategoriesUseCase;
  final BudgetPreferences _budgetPreferences;

  StreamSubscription? _expensesSubscription;
  StreamSubscription? _budgetSubscription;
  StreamSubscription? _categoriesSubscription;
  StreamSubscription? _statsSubscription;

  ExpenseListCubit(
    this._repository,
    this._installmentRepository,
    this._getCategoriesUseCase,
    this._budgetPreferences,
  ) : super(const ExpenseListState()) {
    _init();
  }

  void _init() {
    updateDateRange(DateRangeFilter.thisMonth);
    _loadBudget();
    _loadCategories();
    _loadExpenses();
    _loadHistoricalStats();
  }

  void _loadBudget() {
    _budgetSubscription?.cancel();
    _budgetSubscription = _budgetPreferences.getMonthlyBudget().listen((budget) {
      emit(state.copyWith(monthlyBudget: budget));
    });
  }

  void _loadCategories() {
    _categoriesSubscription?.cancel();
    _categoriesSubscription = _getCategoriesUseCase().listen((categories) {
      emit(state.copyWith(categories: categories));
    });
  }

  void _loadExpenses() {
    _expensesSubscription?.cancel();
    
    _expensesSubscription = Rx.combineLatest2(
      _repository.getFilteredExpenses(
        query: state.searchQuery,
        categoryIds: state.selectedCategoryIds.toList(),
        since: state.startDate,
        until: state.endDate,
        minAmount: state.minAmount,
        maxAmount: state.maxAmount,
        tags: state.selectedTags.toList(),
      ),
      _repository.getDailySpendingBetween(state.startDate, state.endDate),
      (expenses, dailySpending) => Pair(expenses, dailySpending),
    ).listen((pair) {
      final expenses = pair.first;
      final dailySpending = pair.second;
      
      final grouped = _groupExpensesByDate(expenses, dailySpending);
      final periodTotal = expenses
          .filter((e) => !e.isInstallment) 
          .fold(0, (sum, e) => sum + e.amount);

      emit(state.copyWith(
        expenses: expenses,
        groupedExpenses: grouped,
        totalFilteredAmount: periodTotal,
        dailySpending: dailySpending,
        isLoading: false,
      ));
    });
  }

  void _loadHistoricalStats() {
    _statsSubscription?.cancel();
    
    final now = DateTime.now();
    final startOfMonth = DateTime(now.year, now.month, 1).millisecondsSinceEpoch;
    final endOfMonth = DateTime(now.year, now.month + 1, 1).millisecondsSinceEpoch;

    _statsSubscription = Rx.combineLatest2(
      _repository.getTotalSpentBetween(startOfMonth, endOfMonth),
      _installmentRepository.getTotalPaidAmountBetween(startOfMonth, endOfMonth),
      (exp, inst) => (exp ?? 0) + (inst ?? 0),
    ).listen((total) {
      emit(state.copyWith(thisMonthSpent: total));
    });
  }

  Map<String, List<Expense>> _groupExpensesByDate(
    List<Expense> expenses,
    Map<int, int> dailySpendingMap,
  ) {
    final grouped = <String, List<Expense>>{};
    
    for (final expense in expenses) {
      final dateStr = DateFormatterUtils.formatDate(expense.date);
      
      final dt = DateTime.fromMillisecondsSinceEpoch(expense.date);
      final dayStart = DateTime(dt.year, dt.month, dt.day).millisecondsSinceEpoch;
      
      final dayTotal = dailySpendingMap[dayStart] ?? 0;
      final totalStr = CurrencyFormatter.formatAmount(dayTotal);
      final key = "$dateStr \u2022 Total: $totalStr";
      
      grouped.putIfAbsent(key, () => []).add(expense);
    }
    
    return grouped;
  }

  void updateSearchQuery(String query) {
    emit(state.copyWith(searchQuery: query, isLoading: true));
    _loadExpenses();
  }

  void toggleCategoryFilter(int categoryId) {
    final newSelected = Set<int>.from(state.selectedCategoryIds);
    if (newSelected.contains(categoryId)) {
      newSelected.remove(categoryId);
    } else {
      newSelected.add(categoryId);
    }
    emit(state.copyWith(selectedCategoryIds: newSelected, isLoading: true));
    _loadExpenses();
  }

  void updateDateRange(DateRangeFilter filter) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    
    int start = 0;
    int end = 9223372036854775807;

    switch (filter) {
      case DateRangeFilter.sevenDays:
        start = today.subtract(const Duration(days: 7)).millisecondsSinceEpoch;
        end = today.add(const Duration(days: 1)).millisecondsSinceEpoch;
        break;
      case DateRangeFilter.thirtyDays:
        start = today.subtract(const Duration(days: 30)).millisecondsSinceEpoch;
        end = today.add(const Duration(days: 1)).millisecondsSinceEpoch;
        break;
      case DateRangeFilter.thisMonth:
        start = DateTime(now.year, now.month, 1).millisecondsSinceEpoch;
        end = DateTime(now.year, now.month + 1, 1).millisecondsSinceEpoch;
        break;
      case DateRangeFilter.allTime:
        start = 0;
        end = 9223372036854775807;
        break;
      case DateRangeFilter.custom:
        start = state.startDate;
        end = state.endDate;
        break;
    }

    emit(state.copyWith(
      startDate: start,
      endDate: end,
      activeDateFilter: filter,
      isLoading: true,
    ));
    _loadExpenses();
  }

  void deleteExpense(Expense expense) async {
    await _repository.deleteExpense(expense);
  }

  void updateAmountRange(int? min, int? max) {
    emit(state.copyWith(minAmount: min, maxAmount: max, isLoading: true));
    _loadExpenses();
  }

  void clearFilters() {
    emit(state.copyWith(
      selectedCategoryIds: {},
      selectedTags: {},
      minAmount: null,
      maxAmount: null,
      searchQuery: '',
      isLoading: true,
    ));
    _loadExpenses();
  }

  @override
  Future<void> close() {
    _expensesSubscription?.cancel();
    _budgetSubscription?.cancel();
    _categoriesSubscription?.cancel();
    _statsSubscription?.cancel();
    return super.close();
  }
}

class Pair<T1, T2> {
  final T1 first;
  final T2 second;
  Pair(this.first, this.second);
}

extension ListFilter<T> on List<T> {
  List<T> filter(bool Function(T) test) => where(test).toList();
}
