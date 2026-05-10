import 'package:injectable/injectable.dart';
import '../models/category_spent.dart';
import '../repository/expense_repository.dart';
import 'package:rxdart/rxdart.dart';

class StatsOverview {
  final int thisMonthSpent;
  final int lastMonthSpent;
  final int thisYearSpent;
  final int lastYearSpent;
  final List<CategorySpent> spendingByCategory;
  final Map<int, int> trendSpending;

  StatsOverview({
    required this.thisMonthSpent,
    required this.lastMonthSpent,
    required this.thisYearSpent,
    required this.lastYearSpent,
    required this.spendingByCategory,
    required this.trendSpending,
  });
}

@lazySingleton
class GetStatsUseCase {
  final ExpenseRepository _repository;

  GetStatsUseCase(this._repository);

  Stream<StatsOverview> call({int? trendSince, int? trendUntil}) {
    final now = DateTime.now();
    
    final startOfMonth = DateTime(now.year, now.month, 1).millisecondsSinceEpoch;
    final endOfMonth = DateTime(now.year, now.month + 1, 1).millisecondsSinceEpoch;
    
    final lastMonth = DateTime(now.year, now.month - 1, 1);
    final startOfLastMonth = lastMonth.millisecondsSinceEpoch;
    final endOfLastMonth = startOfMonth;

    final startOfYear = DateTime(now.year, 1, 1).millisecondsSinceEpoch;
    final endOfYear = DateTime(now.year + 1, 1, 1).millisecondsSinceEpoch;

    final startOfLastYear = DateTime(now.year - 1, 1, 1).millisecondsSinceEpoch;
    final endOfLastYear = startOfYear;

    final tSince = trendSince ?? startOfMonth;
    final tUntil = trendUntil ?? endOfMonth;

    return Rx.combineLatest6(
      _repository.getTotalSpentBetween(startOfMonth, endOfMonth),
      _repository.getTotalSpentBetween(startOfLastMonth, endOfLastMonth),
      _repository.getTotalSpentBetween(startOfYear, endOfYear),
      _repository.getTotalSpentBetween(startOfLastYear, endOfLastYear),
      _repository.getSpendingByCategoryBetween(startOfMonth, endOfMonth),
      _repository.getDailySpendingBetween(tSince, tUntil),
      (m1, m2, y1, y2, cat, trend) => StatsOverview(
        thisMonthSpent: m1 ?? 0,
        lastMonthSpent: m2 ?? 0,
        thisYearSpent: y1 ?? 0,
        lastYearSpent: y2 ?? 0,
        spendingByCategory: cat,
        trendSpending: trend,
      ),
    );
  }
}
