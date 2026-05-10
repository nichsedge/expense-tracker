import 'package:injectable/injectable.dart';
import '../models/expense.dart';
import '../repository/expense_repository.dart';
import '../repository/installment_repository.dart';
import 'create_installment_plan_use_case.dart';

@lazySingleton
class UpdateExpenseUseCase {
  final ExpenseRepository _repository;
  final InstallmentRepository _installmentRepository;
  final CreateInstallmentPlanUseCase _createInstallmentPlanUseCase;

  UpdateExpenseUseCase(
    this._repository,
    this._installmentRepository,
    this._createInstallmentPlanUseCase,
  );

  Future<void> call(Expense expense, {int? durationMonths}) async {
    final oldExpense = await _repository.getExpenseById(expense.id);
    await _repository.updateExpense(expense);

    // Handle installment transitions
    if (oldExpense?.isInstallment == true && !expense.isInstallment) {
      // Case: Installment -> Regular
      await _installmentRepository.deleteInstallmentByExpenseId(expense.id);
    } else if (expense.isInstallment) {
      // Case: Either already installment or switching to it
      if (durationMonths != null && durationMonths > 0) {
        await _installmentRepository.deleteInstallmentByExpenseId(expense.id);
        await _createInstallmentPlanUseCase(
          expenseId: expense.id,
          totalAmount: expense.amount,
          durationMonths: durationMonths,
          startDate: expense.date,
        );
      }
    }
  }
}
