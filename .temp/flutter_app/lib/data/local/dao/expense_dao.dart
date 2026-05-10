import 'package:drift/drift.dart';
import '../database.dart';
import '../../../domain/models/category_spent.dart';
import 'dart:async';

part 'expense_dao.g.dart';

class ExpenseWithTags {
  final ExpenseEntity expense;
  final List<TagEntity> tags;

  ExpenseWithTags({required this.expense, required this.tags});
}

class DaySpent {
  final int day;
  final int amount;

  DaySpent({required this.day, required this.amount});
}

@DriftAccessor(tables: [Expenses, ExpenseTagRefs, Tags, Categories, Installments, InstallmentItems])
class ExpenseDao extends DatabaseAccessor<AppDatabase> with _$ExpenseDaoMixin {
  ExpenseDao(super.db);

  Stream<List<ExpenseWithTags>> getAllExpenses() {
    final query = select(expenses).join([
      leftOuterJoin(expenseTagRefs, expenseTagRefs.expenseId.equalsExp(expenses.id)),
      leftOuterJoin(tags, tags.id.equalsExp(expenseTagRefs.tagId)),
    ])..orderBy([OrderingTerm.desc(expenses.date)]);

    return query.watch().map((rows) {
      final grouped = <int, ExpenseWithTags>{};
      
      for (final row in rows) {
        final expense = row.readTable(expenses);
        final tag = row.readTableOrNull(tags);
        
        final current = grouped.putIfAbsent(
          expense.id, 
          () => ExpenseWithTags(expense: expense, tags: [])
        );
        
        if (tag != null && !current.tags.any((t) => t.id == tag.id)) {
          current.tags.add(tag);
        }
      }
      
      return grouped.values.toList();
    });
  }

  Future<int> insertExpense(Insertable<ExpenseEntity> expense) {
    return into(expenses).insert(expense, mode: InsertMode.replace);
  }

  Future<void> updateExpense(Insertable<ExpenseEntity> expense) {
    return update(expenses).replace(expense);
  }

  Future<void> deleteExpense(Insertable<ExpenseEntity> expense) {
    return delete(expenses).delete(expense);
  }

  Future<ExpenseWithTags?> getExpenseById(int id) async {
    final query = select(expenses).join([
      leftOuterJoin(expenseTagRefs, expenseTagRefs.expenseId.equalsExp(expenses.id)),
      leftOuterJoin(tags, tags.id.equalsExp(expenseTagRefs.tagId)),
    ])..where(expenses.id.equals(id));

    final rows = await query.get();
    if (rows.isEmpty) return null;

    final expense = rows.first.readTable(expenses);
    final tagList = <TagEntity>[];
    for (final row in rows) {
      final tag = row.readTableOrNull(tags);
      if (tag != null) tagList.add(tag);
    }

    return ExpenseWithTags(expense: expense, tags: tagList);
  }

  Future<List<String>> getItemNameSuggestions(String query) {
    final q = selectOnly(expenses, distinct: true)
      ..addColumns([expenses.itemName])
      ..where(expenses.itemName.like('%$query%'))
      ..limit(5);
    return q.map((row) => row.read(expenses.itemName)!).get();
  }

  Future<List<String>> getMerchantSuggestions(String query) {
    final q = selectOnly(expenses, distinct: true)
      ..addColumns([expenses.merchant])
      ..where(expenses.merchant.like('%$query%') & expenses.merchant.isNotNull() & expenses.merchant.isNotValue(''))
      ..limit(5);
    return q.map((row) => row.read(expenses.merchant)!).get();
  }

  Future<void> insertExpenseTagCrossRefs(List<Insertable<ExpenseTagCrossRef>> crossRefs) async {
    await batch((batch) {
      batch.insertAll(expenseTagRefs, crossRefs, mode: InsertMode.replace);
    });
  }

  Future<void> deleteExpenseTagRefs(int expenseId) {
    return (delete(expenseTagRefs)..where((t) => t.expenseId.equals(expenseId))).go();
  }

  Stream<List<String>> getAllTags() {
    final q = selectOnly(tags, distinct: true)..addColumns([tags.name]);
    return q.watch().map((rows) => rows.map((row) => row.read(tags.name)!).toList());
  }

  Stream<List<ExpenseWithTags>> getFilteredExpenses({
    String? query,
    List<int>? categoryIds,
    int? since,
    int? until,
    int? minAmount,
    int? maxAmount,
    List<String>? tags,
  }) {
    final q = select(expenses).join([
      leftOuterJoin(expenseTagRefs, expenseTagRefs.expenseId.equalsExp(expenses.id)),
      leftOuterJoin(this.tags, this.tags.id.equalsExp(expenseTagRefs.tagId)),
    ]);

    if (query != null && query.isNotEmpty) {
      q.where(expenses.itemName.like('%$query%') | expenses.merchant.like('%$query%'));
    }
    if (categoryIds != null && categoryIds.isNotEmpty) {
      q.where(expenses.categoryId.isIn(categoryIds));
    }
    if (since != null) {
      q.where(CustomExpression<bool>('expenses.date >= $since'));
    }
    if (until != null) {
      q.where(CustomExpression<bool>('expenses.date < $until'));
    }
    if (minAmount != null) {
      q.where(CustomExpression<bool>('expenses.final_price >= $minAmount'));
    }
    if (maxAmount != null) {
      q.where(CustomExpression<bool>('expenses.final_price <= $maxAmount'));
    }
    
    q.orderBy([OrderingTerm.desc(expenses.date)]);

    return q.watch().map((rows) {
      final grouped = <int, ExpenseWithTags>{};
      for (final row in rows) {
        final expense = row.readTable(expenses);
        final tag = row.readTableOrNull(this.tags);
        final current = grouped.putIfAbsent(expense.id, () => ExpenseWithTags(expense: expense, tags: []));
        if (tag != null && !current.tags.any((t) => t.id == tag.id)) {
          current.tags.add(tag);
        }
      }
      return grouped.values.toList();
    });
  }

  Stream<int?> getTotalSpentBetween(int since, int until) {
    final query = selectOnly(expenses)
      ..addColumns([expenses.finalPrice.sum()])
      ..where(expenses.isInstallment.equals(false) & 
             CustomExpression<bool>('expenses.date >= $since') & 
             CustomExpression<bool>('expenses.date < $until'));
    return query.map((row) => row.read(expenses.finalPrice.sum())).watchSingle();
  }

  Stream<Map<int, int>> getDailySpendingBetween(int since, int until) {
    final query = selectOnly(expenses)
      ..addColumns([expenses.date, expenses.finalPrice.sum()])
      ..where(expenses.isInstallment.equals(false) & 
             CustomExpression<bool>('expenses.date >= $since') & 
             CustomExpression<bool>('expenses.date < $until'))
      ..groupBy([expenses.date]);

    return query.watch().map((rows) {
      final map = <int, int>{};
      for (final row in rows) {
        final date = row.read(expenses.date)!;
        final sum = row.read(expenses.finalPrice.sum())!;
        final dt = DateTime.fromMillisecondsSinceEpoch(date);
        final dayStart = DateTime(dt.year, dt.month, dt.day).millisecondsSinceEpoch;
        map[dayStart] = (map[dayStart] ?? 0) + sum;
      }
      return map;
    });
  }

  Stream<List<CategorySpent>> getSpendingByCategoryBetween(int since, int until) {
    final query = selectOnly(expenses).join([
      innerJoin(categories, categories.id.equalsExp(expenses.categoryId)),
    ])
      ..addColumns([categories.id, categories.name, categories.icon, expenses.finalPrice.sum()])
      ..where(expenses.isInstallment.equals(false) & 
             CustomExpression<bool>('expenses.date >= $since') & 
             CustomExpression<bool>('expenses.date < $until'))
      ..groupBy([categories.id]);

    return query.watch().map((rows) {
      return rows.map((row) {
        return CategorySpent(
          categoryId: row.read(categories.id)!,
          categoryName: row.read(categories.name)!,
          categoryIcon: row.read(categories.icon)!,
          totalAmount: row.read(expenses.finalPrice.sum())!,
        );
      }).toList();
    });
  }
}
