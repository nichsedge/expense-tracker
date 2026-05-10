import '../models/installment.dart';
import '../models/installment_item.dart';

abstract class InstallmentRepository {
  Stream<List<Installment>> getAllInstallments();
  Future<int> createInstallment(Installment installment);
  Future<void> createInstallmentItems({
    required int installmentId,
    required int durationMonths,
    required int totalAmount,
    required int startDate,
  });
  Future<void> deleteInstallmentByExpenseId(int expenseId);
  Stream<int?> getTotalPaidAmountBetween(int since, int until);
  Stream<List<InstallmentItem>> getItemsForInstallment(int installmentId);
  Future<void> updateInstallmentItemStatus(int itemId, String status);
  Stream<int?> getTotalMonthlyDue(int since, int until);
  Stream<int?> getTotalRemainingBalance();
  Future<Installment?> getInstallmentByExpenseId(int expenseId);
}
