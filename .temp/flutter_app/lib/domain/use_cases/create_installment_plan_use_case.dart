import 'package:injectable/injectable.dart';
import '../models/installment.dart';
import '../repository/installment_repository.dart';

@lazySingleton
class CreateInstallmentPlanUseCase {
  final InstallmentRepository _installmentRepository;

  CreateInstallmentPlanUseCase(this._installmentRepository);

  Future<void> call({
    required int expenseId,
    required int totalAmount,
    required int durationMonths,
    required int startDate,
  }) async {
    if (durationMonths <= 0) return;

    final monthlyPayment = (totalAmount / durationMonths).floor();

    final installment = Installment(
      expenseId: expenseId,
      totalAmount: totalAmount,
      monthlyPayment: monthlyPayment,
      durationMonths: durationMonths,
      remainingBalance: totalAmount,
      nextDueDate: startDate,
      status: "Active",
      createdAt: DateTime.now().millisecondsSinceEpoch,
    );

    final installmentId = await _installmentRepository.createInstallment(installment);

    await _installmentRepository.createInstallmentItems(
      installmentId: installmentId,
      durationMonths: durationMonths,
      totalAmount: totalAmount,
      startDate: startDate,
    );
  }
}
