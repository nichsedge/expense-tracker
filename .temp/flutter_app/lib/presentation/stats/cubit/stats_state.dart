import 'package:equatable/equatable.dart';
import '../../../domain/models/category_spent.dart';

enum TrendPeriod { daily, weekly, monthly, quarterly, yearly }

class StatsState extends Equatable {
  final int thisMonthSpent;
  final int lastMonthSpent;
  final int thisYearSpent;
  final int lastYearSpent;
  final List<CategorySpent> spendingByCategory;
  final Map<int, int> trendSpending;
  final TrendPeriod selectedTrendPeriod;
  final bool isLoading;
  final String? error;

  const StatsState({
    this.thisMonthSpent = 0,
    this.lastMonthSpent = 0,
    this.thisYearSpent = 0,
    this.lastYearSpent = 0,
    this.spendingByCategory = const [],
    this.trendSpending = const {},
    this.selectedTrendPeriod = TrendPeriod.daily,
    this.isLoading = false,
    this.error,
  });

  @override
  List<Object?> get props => [
    thisMonthSpent,
    lastMonthSpent,
    thisYearSpent,
    lastYearSpent,
    spendingByCategory,
    trendSpending,
    selectedTrendPeriod,
    isLoading,
    error,
  ];

  StatsState copyWith({
    int? thisMonthSpent,
    int? lastMonthSpent,
    int? thisYearSpent,
    int? lastYearSpent,
    List<CategorySpent>? spendingByCategory,
    Map<int, int>? trendSpending,
    TrendPeriod? selectedTrendPeriod,
    bool? isLoading,
    String? error,
  }) {
    return StatsState(
      thisMonthSpent: thisMonthSpent ?? this.thisMonthSpent,
      lastMonthSpent: lastMonthSpent ?? this.lastMonthSpent,
      thisYearSpent: thisYearSpent ?? this.thisYearSpent,
      lastYearSpent: lastYearSpent ?? this.lastYearSpent,
      spendingByCategory: spendingByCategory ?? this.spendingByCategory,
      trendSpending: trendSpending ?? this.trendSpending,
      selectedTrendPeriod: selectedTrendPeriod ?? this.selectedTrendPeriod,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}
