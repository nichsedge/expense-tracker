import 'package:equatable/equatable.dart';
import '../../../domain/models/expense.dart';
import '../../../domain/models/category.dart';

enum DateRangeFilter {
  sevenDays,
  thirtyDays,
  thisMonth,
  allTime,
  custom
}

class ExpenseListState extends Equatable {
  final List<Expense> expenses;
  final Map<String, List<Expense>> groupedExpenses;
  final int thisMonthSpent;
  final int totalFilteredAmount;
  final int startDate;
  final int endDate;
  final DateRangeFilter activeDateFilter;
  final bool isLoading;
  final String? error;
  final List<Category> categories;
  final List<String> availableTags;
  final Set<String> selectedTags;
  final String searchQuery;
  final Set<int> selectedCategoryIds;
  final int? minAmount;
  final int? maxAmount;
  final Map<int, int> dailySpending;
  final int monthlyBudget;

  const ExpenseListState({
    this.expenses = const [],
    this.groupedExpenses = const {},
    this.thisMonthSpent = 0,
    this.totalFilteredAmount = 0,
    this.startDate = 0,
    this.endDate = 9223372036854775807, // Long.MAX_VALUE
    this.activeDateFilter = DateRangeFilter.thisMonth,
    this.isLoading = true,
    this.error,
    this.categories = const [],
    this.availableTags = const [],
    this.selectedTags = const {},
    this.searchQuery = "",
    this.selectedCategoryIds = const {},
    this.minAmount,
    this.maxAmount,
    this.dailySpending = const {},
    this.monthlyBudget = 0,
  });

  ExpenseListState copyWith({
    List<Expense>? expenses,
    Map<String, List<Expense>>? groupedExpenses,
    int? thisMonthSpent,
    int? totalFilteredAmount,
    int? startDate,
    int? endDate,
    DateRangeFilter? activeDateFilter,
    bool? isLoading,
    String? error,
    List<Category>? categories,
    List<String>? availableTags,
    Set<String>? selectedTags,
    String? searchQuery,
    Set<int>? selectedCategoryIds,
    int? minAmount,
    int? maxAmount,
    Map<int, int>? dailySpending,
    int? monthlyBudget,
  }) {
    return ExpenseListState(
      expenses: expenses ?? this.expenses,
      groupedExpenses: groupedExpenses ?? this.groupedExpenses,
      thisMonthSpent: thisMonthSpent ?? this.thisMonthSpent,
      totalFilteredAmount: totalFilteredAmount ?? this.totalFilteredAmount,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      activeDateFilter: activeDateFilter ?? this.activeDateFilter,
      isLoading: isLoading ?? this.isLoading,
      error: error ?? this.error,
      categories: categories ?? this.categories,
      availableTags: availableTags ?? this.availableTags,
      selectedTags: selectedTags ?? this.selectedTags,
      searchQuery: searchQuery ?? this.searchQuery,
      selectedCategoryIds: selectedCategoryIds ?? this.selectedCategoryIds,
      minAmount: minAmount ?? this.minAmount,
      maxAmount: maxAmount ?? this.maxAmount,
      dailySpending: dailySpending ?? this.dailySpending,
      monthlyBudget: monthlyBudget ?? this.monthlyBudget,
    );
  }

  @override
  List<Object?> get props => [
        expenses,
        groupedExpenses,
        thisMonthSpent,
        totalFilteredAmount,
        startDate,
        endDate,
        activeDateFilter,
        isLoading,
        error,
        categories,
        availableTags,
        selectedTags,
        searchQuery,
        selectedCategoryIds,
        minAmount,
        maxAmount,
        dailySpending,
        monthlyBudget,
      ];
}
