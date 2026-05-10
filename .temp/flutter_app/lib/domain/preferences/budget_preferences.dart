abstract class BudgetPreferences {
  Stream<int> getMonthlyBudget();
  Future<void> setMonthlyBudget(int budget);
}
