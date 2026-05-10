// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'installment_dao.dart';

// ignore_for_file: type=lint
mixin _$InstallmentDaoMixin on DatabaseAccessor<AppDatabase> {
  $ExpensesTable get expenses => attachedDatabase.expenses;
  $InstallmentsTable get installments => attachedDatabase.installments;
  $InstallmentItemsTable get installmentItems =>
      attachedDatabase.installmentItems;
  InstallmentDaoManager get managers => InstallmentDaoManager(this);
}

class InstallmentDaoManager {
  final _$InstallmentDaoMixin _db;
  InstallmentDaoManager(this._db);
  $$ExpensesTableTableManager get expenses =>
      $$ExpensesTableTableManager(_db.attachedDatabase, _db.expenses);
  $$InstallmentsTableTableManager get installments =>
      $$InstallmentsTableTableManager(_db.attachedDatabase, _db.installments);
  $$InstallmentItemsTableTableManager get installmentItems =>
      $$InstallmentItemsTableTableManager(
        _db.attachedDatabase,
        _db.installmentItems,
      );
}
