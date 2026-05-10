import 'package:drift/drift.dart';
import 'dart:io';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:sqlite3/sqlite3.dart';

part 'database.g.dart';

@DataClassName('ExpenseEntity')
class Expenses extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get date => integer()();
  TextColumn get platform => text().nullable()();
  TextColumn get merchant => text().nullable()();
  TextColumn get itemName => text().named('item_name')();
  IntColumn get quantity => integer()();
  IntColumn get originalPrice => integer().named('original_price')();
  IntColumn get finalPrice => integer().named('final_price')();
  IntColumn get categoryId => integer().named('category_id')();
  TextColumn get status => text()();
  BoolColumn get isRecurring => boolean().named('is_recurring')();
  BoolColumn get isInstallment => boolean().named('is_installment').withDefault(const Constant(false))();
  IntColumn get createdAt => integer().named('created_at')();
  IntColumn get updatedAt => integer().named('updated_at')();
}

@DataClassName('CategoryEntity')
class Categories extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get icon => text()();
  IntColumn get orderIndex => integer().withDefault(const Constant(0))();
}

@DataClassName('TagEntity')
class Tags extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  IntColumn get orderIndex => integer().withDefault(const Constant(0))();
}

@DataClassName('ExpenseTagCrossRef')
@TableIndex(name: 'index_expense_tag_ref_expenseId', columns: {#expenseId})
@TableIndex(name: 'index_expense_tag_ref_tagId', columns: {#tagId})
class ExpenseTagRefs extends Table {
  @override
  String get tableName => 'expense_tag_ref';

  IntColumn get expenseId => integer().named('expenseId').references(Expenses, #id, onDelete: KeyAction.cascade)();
  IntColumn get tagId => integer().named('tagId').references(Tags, #id, onDelete: KeyAction.cascade)();

  @override
  Set<Column> get primaryKey => {expenseId, tagId};
}

@DataClassName('InstallmentEntity')
@TableIndex(name: 'index_installments_expense_id', columns: {#expenseId})
class Installments extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get expenseId => integer().named('expense_id').references(Expenses, #id, onDelete: KeyAction.cascade)();
  IntColumn get totalAmount => integer().named('total_amount')();
  IntColumn get monthlyPayment => integer().named('monthly_payment')();
  IntColumn get durationMonths => integer().named('duration_months')();
  IntColumn get remainingBalance => integer().named('remaining_balance')();
  IntColumn get nextDueDate => integer().named('next_due_date')();
  TextColumn get status => text()();
  IntColumn get createdAt => integer().named('created_at')();
}

@DataClassName('InstallmentItemEntity')
@TableIndex(name: 'index_installment_items_installment_id', columns: {#installmentId})
class InstallmentItems extends Table {
  @override
  String get tableName => 'installment_items';

  IntColumn get id => integer().autoIncrement()();
  IntColumn get installmentId => integer().named('installment_id').references(Installments, #id, onDelete: KeyAction.cascade)();
  IntColumn get amount => integer()();
  IntColumn get dueDate => integer().named('due_date')();
  TextColumn get status => text()();
  IntColumn get monthNumber => integer().named('month_number')();
}

@DriftDatabase(tables: [Expenses, Categories, Tags, ExpenseTagRefs, Installments, InstallmentItems])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 7;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (Migrator m) async {
        await m.createAll();
        
        // Seed initial categories
        await batch((batch) {
          batch.insertAll(categories, [
            CategoriesCompanion.insert(name: 'Food', icon: 'food'),
            CategoriesCompanion.insert(name: 'Transport', icon: 'transport'),
            CategoriesCompanion.insert(name: 'Shopping', icon: 'shopping'),
            CategoriesCompanion.insert(name: 'Entertainment', icon: 'entertainment'),
            CategoriesCompanion.insert(name: 'Bills', icon: 'bills'),
          ]);
        });
      },
      onUpgrade: (Migrator m, int from, int to) async {
        if (from <= 6) {
          // Room DB is at version 6. Add orderIndex to categories and tags.
          await m.addColumn(categories, categories.orderIndex);
          await m.addColumn(tags, tags.orderIndex);
        }
      },
    );
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'expense_tracker.sqlite'));

    final cachebase = (await getTemporaryDirectory()).path;
    sqlite3.tempDirectory = cachebase;

    return NativeDatabase.createInBackground(file);
  });
}
