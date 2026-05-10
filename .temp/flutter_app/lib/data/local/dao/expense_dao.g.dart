// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'expense_dao.dart';

// ignore_for_file: type=lint
mixin _$ExpenseDaoMixin on DatabaseAccessor<AppDatabase> {
  $ExpensesTable get expenses => attachedDatabase.expenses;
  $TagsTable get tags => attachedDatabase.tags;
  $ExpenseTagRefsTable get expenseTagRefs => attachedDatabase.expenseTagRefs;
  $CategoriesTable get categories => attachedDatabase.categories;
  $InstallmentsTable get installments => attachedDatabase.installments;
  $InstallmentItemsTable get installmentItems =>
      attachedDatabase.installmentItems;
  ExpenseDaoManager get managers => ExpenseDaoManager(this);
}

class ExpenseDaoManager {
  final _$ExpenseDaoMixin _db;
  ExpenseDaoManager(this._db);
  $$ExpensesTableTableManager get expenses =>
      $$ExpensesTableTableManager(_db.attachedDatabase, _db.expenses);
  $$TagsTableTableManager get tags =>
      $$TagsTableTableManager(_db.attachedDatabase, _db.tags);
  $$ExpenseTagRefsTableTableManager get expenseTagRefs =>
      $$ExpenseTagRefsTableTableManager(
        _db.attachedDatabase,
        _db.expenseTagRefs,
      );
  $$CategoriesTableTableManager get categories =>
      $$CategoriesTableTableManager(_db.attachedDatabase, _db.categories);
  $$InstallmentsTableTableManager get installments =>
      $$InstallmentsTableTableManager(_db.attachedDatabase, _db.installments);
  $$InstallmentItemsTableTableManager get installmentItems =>
      $$InstallmentItemsTableTableManager(
        _db.attachedDatabase,
        _db.installmentItems,
      );
}
