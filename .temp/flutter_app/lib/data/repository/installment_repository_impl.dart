import '../../domain/models/installment.dart';
import '../../domain/models/installment_item.dart';
import '../../domain/repository/installment_repository.dart';
import '../local/dao/installment_dao.dart';
import '../local/database.dart';
// import 'package:drift/drift.dart';

class InstallmentRepositoryImpl implements InstallmentRepository {
  final InstallmentDao _installmentDao;

  InstallmentRepositoryImpl(this._installmentDao);

  @override
  Stream<List<Installment>> getAllInstallments() {
    return _installmentDao.getAllInstallmentsWithExpense().map((items) {
      return items.map((e) => Installment(
        id: e.installment.id,
        expenseId: e.installment.expenseId,
        totalAmount: e.installment.totalAmount,
        monthlyPayment: e.installment.monthlyPayment,
        durationMonths: e.installment.durationMonths,
        remainingBalance: e.installment.remainingBalance,
        nextDueDate: e.installment.nextDueDate,
        status: e.installment.status,
        createdAt: e.installment.createdAt,
        expenseName: e.expense.itemName,
        expenseDate: e.expense.date,
      )).toList();
    });
  }

  @override
  Future<int> createInstallment(Installment installment) async {
    final entity = InstallmentsCompanion.insert(
      expenseId: installment.expenseId,
      totalAmount: installment.totalAmount,
      monthlyPayment: installment.monthlyPayment,
      durationMonths: installment.durationMonths,
      remainingBalance: installment.remainingBalance,
      nextDueDate: installment.nextDueDate,
      status: installment.status,
      createdAt: installment.createdAt,
    );
    return _installmentDao.insertInstallment(entity);
  }

  @override
  Future<void> createInstallmentItems({
    required int installmentId,
    required int durationMonths,
    required int totalAmount,
    required int startDate,
  }) async {
    final monthlyPayment = (totalAmount / durationMonths).floor();
    final items = <InstallmentItemsCompanion>[];
    
    for (int i = 0; i < durationMonths; i++) {
      final dueDate = DateTime.fromMillisecondsSinceEpoch(startDate);
      final itemDate = DateTime(dueDate.year, dueDate.month + i, dueDate.day).millisecondsSinceEpoch;
      
      items.add(InstallmentItemsCompanion.insert(
        installmentId: installmentId,
        amount: monthlyPayment,
        dueDate: itemDate,
        status: 'Pending',
        monthNumber: i + 1,
      ));
    }
    
    await _installmentDao.insertInstallmentItems(items);
  }

  @override
  Future<void> deleteInstallmentByExpenseId(int expenseId) async {
    await _installmentDao.deleteInstallmentByExpenseId(expenseId);
  }

  @override
  Stream<int?> getTotalPaidAmountBetween(int since, int until) {
    return _installmentDao.getTotalPaidAmountBetween(since, until);
  }

  @override
  Stream<List<InstallmentItem>> getItemsForInstallment(int installmentId) {
    return _installmentDao.getItemsForInstallment(installmentId).map((entities) {
      return entities.map((e) => InstallmentItem(
        id: e.id,
        installmentId: e.installmentId,
        amount: e.amount,
        dueDate: e.dueDate,
        status: e.status,
        monthNumber: e.monthNumber,
      )).toList();
    });
  }

  @override
  Future<void> updateInstallmentItemStatus(int itemId, String status) async {
    await _installmentDao.updateInstallmentItemStatus(itemId, status);
  }

  @override
  Stream<int?> getTotalMonthlyDue(int since, int until) {
    return _installmentDao.getTotalMonthlyDue(since, until);
  }

  @override
  Stream<int?> getTotalRemainingBalance() {
    return _installmentDao.getTotalRemainingBalance();
  }

  @override
  Future<Installment?> getInstallmentByExpenseId(int expenseId) async {
    final entity = await _installmentDao.getInstallmentByExpenseId(expenseId);
    if (entity == null) return null;
    return Installment(
      id: entity.id,
      expenseId: entity.expenseId,
      totalAmount: entity.totalAmount,
      monthlyPayment: entity.monthlyPayment,
      durationMonths: entity.durationMonths,
      remainingBalance: entity.remainingBalance,
      nextDueDate: entity.nextDueDate,
      status: entity.status,
      createdAt: entity.createdAt,
    );
  }
}
