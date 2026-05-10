import 'package:injectable/injectable.dart';
import '../models/expense.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class GetExpenseByIdUseCase {
  final ExpenseRepository _repository;

  GetExpenseByIdUseCase(this._repository);

  Future<Expense?> call(int id) {
    return _repository.getExpenseById(id);
  }
}
