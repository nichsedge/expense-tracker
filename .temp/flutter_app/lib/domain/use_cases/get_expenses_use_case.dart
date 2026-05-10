import 'package:injectable/injectable.dart';
import '../models/expense.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class GetExpensesUseCase {
  final ExpenseRepository _repository;

  GetExpensesUseCase(this._repository);

  Stream<List<Expense>> call() {
    return _repository.getAllExpenses();
  }
}
