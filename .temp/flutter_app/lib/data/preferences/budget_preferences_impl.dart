import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';
import '../../domain/preferences/budget_preferences.dart';
import 'package:injectable/injectable.dart';

@LazySingleton(as: BudgetPreferences)
class BudgetPreferencesImpl implements BudgetPreferences {
  static const _keyMonthlyBudget = 'monthly_budget';
  final _controller = StreamController<int>.broadcast();

  BudgetPreferencesImpl() {
    _init();
  }

  Future<void> _init() async {
    final prefs = await SharedPreferences.getInstance();
    _controller.add(prefs.getInt(_keyMonthlyBudget) ?? 0);
  }

  @override
  Stream<int> getMonthlyBudget() {
    return _controller.stream;
  }

  @override
  Future<void> setMonthlyBudget(int budget) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_keyMonthlyBudget, budget);
    _controller.add(budget);
  }
}
