import '../models/expense.dart';
import '../repository/expense_repository.dart';

class GetExpensesUseCase {
  final ExpenseRepository _repository;

  GetExpensesUseCase(this._repository);

  Stream<List<Expense>> execute() {
    return _repository.getAllExpenses();
  }
}
