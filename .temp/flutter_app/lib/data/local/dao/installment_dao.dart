import 'package:drift/drift.dart';
import '../database.dart';

part 'installment_dao.g.dart';

class InstallmentWithItems {
  final InstallmentEntity installment;
  final List<InstallmentItemEntity> items;
  
  InstallmentWithItems({required this.installment, required this.items});
}

class InstallmentWithExpense {
  final InstallmentEntity installment;
  final ExpenseEntity expense;

  InstallmentWithExpense({required this.installment, required this.expense});
}

@DriftAccessor(tables: [Installments, InstallmentItems, Expenses])
class InstallmentDao extends DatabaseAccessor<AppDatabase> with _$InstallmentDaoMixin {
  InstallmentDao(super.db);

  Future<int> insertInstallment(Insertable<InstallmentEntity> installment) {
    return into(installments).insert(installment, mode: InsertMode.replace);
  }

  Future<void> insertInstallmentItems(List<Insertable<InstallmentItemEntity>> items) async {
    await batch((batch) {
      batch.insertAll(installmentItems, items, mode: InsertMode.replace);
    });
  }

  Stream<List<InstallmentWithExpense>> getAllInstallmentsWithExpense() {
    final query = select(installments).join([
      innerJoin(expenses, expenses.id.equalsExp(installments.expenseId)),
    ])..orderBy([OrderingTerm.desc(installments.createdAt)]);

    return query.watch().map((rows) {
      return rows.map((row) {
        return InstallmentWithExpense(
          installment: row.readTable(installments),
          expense: row.readTable(expenses),
        );
      }).toList();
    });
  }

  Stream<List<InstallmentEntity>> getAllInstallments() {
    return (select(installments)..orderBy([(t) => OrderingTerm(expression: t.createdAt, mode: OrderingMode.desc)])).watch();
  }

  Future<void> deleteInstallmentByExpenseId(int expenseId) {
    return (delete(installments)..where((t) => t.expenseId.equals(expenseId))).go();
  }

  Future<void> insertInstallmentItem(Insertable<InstallmentItemEntity> item) {
    return into(installmentItems).insert(item, mode: InsertMode.replace);
  }

  Future<int> getInstallmentCount() async {
    final countExp = installments.id.count();
    final query = selectOnly(installments)..addColumns([countExp]);
    final result = await query.getSingle();
    return result.read(countExp) ?? 0;
  }

  Stream<int?> getTotalPaidAmountBetween(int since, int until) {
    final query = selectOnly(installmentItems)
      ..addColumns([installmentItems.amount.sum()])
      ..where(installmentItems.status.equals('Paid') & 
             CustomExpression<bool>('installment_items.due_date >= $since') & 
             CustomExpression<bool>('installment_items.due_date < $until'));
    return query.map((row) => row.read(installmentItems.amount.sum())).watchSingle();
  }

  Stream<List<InstallmentItemEntity>> getItemsForInstallment(int installmentId) {
    return (select(installmentItems)..where((t) => t.installmentId.equals(installmentId))).watch();
  }

  Future<void> updateInstallmentItemStatus(int itemId, String status) {
    return (update(installmentItems)..where((t) => t.id.equals(itemId))).write(InstallmentItemsCompanion(status: Value(status)));
  }

  Stream<int?> getTotalMonthlyDue(int since, int until) {
    final query = selectOnly(installmentItems)
      ..addColumns([installmentItems.amount.sum()])
      ..where(installmentItems.status.equals('Pending') & 
             CustomExpression<bool>('installment_items.due_date >= $since') & 
             CustomExpression<bool>('installment_items.due_date < $until'));
    return query.map((row) => row.read(installmentItems.amount.sum())).watchSingle();
  }

  Stream<int?> getTotalRemainingBalance() {
    final query = selectOnly(installmentItems)
      ..addColumns([installmentItems.amount.sum()])
      ..where(installmentItems.status.equals('Pending'));
    return query.map((row) => row.read(installmentItems.amount.sum())).watchSingle();
  }

  Future<InstallmentEntity?> getInstallmentByExpenseId(int expenseId) {
    return (select(installments)..where((t) => t.expenseId.equals(expenseId))).getSingleOrNull();
  }
}
