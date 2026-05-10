import 'package:injectable/injectable.dart';
import '../models/expense.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class AddExpenseUseCase {
  final ExpenseRepository _repository;

  AddExpenseUseCase(this._repository);

  Future<int> call(Expense expense) {
    return _repository.addExpense(expense);
  }
}
