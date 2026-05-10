import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../../domain/use_cases/get_stats_use_case.dart';
import 'stats_state.dart';

@injectable
class StatsCubit extends Cubit<StatsState> {
  final GetStatsUseCase _getStatsUseCase;
  StreamSubscription? _statsSubscription;

  StatsCubit(this._getStatsUseCase) : super(const StatsState()) {
    _init();
  }

  void _init() {
    _loadStats();
  }

  void _loadStats() {
    _statsSubscription?.cancel();
    emit(state.copyWith(isLoading: true));

    final now = DateTime.now();
    int? since;
    int? until;

    switch (state.selectedTrendPeriod) {
      case TrendPeriod.daily:
        since = DateTime(now.year, now.month, now.day).subtract(const Duration(days: 7)).millisecondsSinceEpoch;
        break;
      case TrendPeriod.weekly:
        since = DateTime(now.year, now.month, now.day).subtract(const Duration(days: 30)).millisecondsSinceEpoch;
        break;
      case TrendPeriod.monthly:
        since = DateTime(now.year, 1, 1).millisecondsSinceEpoch;
        break;
      case TrendPeriod.quarterly:
        since = DateTime(now.year - 1, now.month, 1).millisecondsSinceEpoch;
        break;
      case TrendPeriod.yearly:
        since = DateTime(now.year - 5, 1, 1).millisecondsSinceEpoch;
        break;
    }

    _statsSubscription = _getStatsUseCase(trendSince: since, trendUntil: until).listen((stats) {
      emit(state.copyWith(
        thisMonthSpent: stats.thisMonthSpent,
        lastMonthSpent: stats.lastMonthSpent,
        thisYearSpent: stats.thisYearSpent,
        lastYearSpent: stats.lastYearSpent,
        spendingByCategory: stats.spendingByCategory,
        trendSpending: stats.trendSpending,
        isLoading: false,
      ));
    }, onError: (e) {
      emit(state.copyWith(isLoading: false, error: e.toString()));
    });
  }

  void onTrendPeriodSelected(TrendPeriod period) {
    emit(state.copyWith(selectedTrendPeriod: period));
    _loadStats();
  }

  @override
  Future<void> close() {
    _statsSubscription?.cancel();
    return super.close();
  }
}
