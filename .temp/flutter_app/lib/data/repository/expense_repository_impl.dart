import '../../domain/models/expense.dart';
import '../../domain/models/category.dart';
import '../../domain/models/category_spent.dart';
import '../../domain/models/tag.dart';
import '../../domain/repository/expense_repository.dart';
import '../local/dao/expense_dao.dart';
import '../local/dao/category_dao.dart';
import '../local/dao/tag_dao.dart';
import '../local/database.dart';
import 'package:drift/drift.dart';

class ExpenseRepositoryImpl implements ExpenseRepository {
  final ExpenseDao _expenseDao;
  final CategoryDao _categoryDao;
  final TagDao _tagDao;

  ExpenseRepositoryImpl(this._expenseDao, this._categoryDao, this._tagDao);

  @override
  Stream<List<Expense>> getAllExpenses() {
    return _expenseDao.getAllExpenses().map((List<ExpenseWithTags> items) {
      return items.map((e) {
        return Expense(
          id: e.expense.id,
          date: e.expense.date,
          itemName: e.expense.itemName,
          amount: e.expense.finalPrice,
          categoryId: e.expense.categoryId,
          isRecurring: e.expense.isRecurring,
          isInstallment: e.expense.isInstallment,
          merchant: e.expense.merchant,
          quantity: e.expense.quantity,
          tags: e.tags.map((t) => t.name).toList(),
        );
      }).toList();
    });
  }

  @override
  Future<List<Expense>> getAllExpensesFuture() async {
    final items = await _expenseDao.getAllExpenses().first;
    return items.map((e) => Expense(
      id: e.expense.id,
      date: e.expense.date,
      itemName: e.expense.itemName,
      amount: e.expense.finalPrice,
      categoryId: e.expense.categoryId,
      isRecurring: e.expense.isRecurring,
      isInstallment: e.expense.isInstallment,
      merchant: e.expense.merchant,
      quantity: e.expense.quantity,
      tags: e.tags.map((t) => t.name).toList(),
    )).toList();
  }

  @override
  Stream<List<Category>> getAllCategories() {
    return _categoryDao.getAllCategories().map((entities) {
      return entities.map((e) => Category(
        id: e.id,
        name: e.name,
        icon: e.icon,
        orderIndex: e.orderIndex,
      )).toList();
    });
  }

  @override
  Future<Expense?> getExpenseById(int id) async {
    final e = await _expenseDao.getExpenseById(id);
    if (e == null) return null;
    return Expense(
      id: e.expense.id,
      date: e.expense.date,
      itemName: e.expense.itemName,
      amount: e.expense.finalPrice,
      categoryId: e.expense.categoryId,
      isRecurring: e.expense.isRecurring,
      isInstallment: e.expense.isInstallment,
      merchant: e.expense.merchant,
      quantity: e.expense.quantity,
      tags: e.tags.map((t) => t.name).toList(),
    );
  }

  @override
  Future<int> addExpense(Expense expense) async {
    final entity = ExpensesCompanion.insert(
      date: expense.date,
      itemName: expense.itemName,
      quantity: expense.quantity,
      originalPrice: expense.amount,
      finalPrice: expense.amount,
      categoryId: expense.categoryId,
      status: 'Paid',
      isRecurring: expense.isRecurring,
      isInstallment: Value(expense.isInstallment),
      createdAt: DateTime.now().millisecondsSinceEpoch,
      updatedAt: DateTime.now().millisecondsSinceEpoch,
      platform: const Value(null),
      merchant: Value(expense.merchant),
    );
    return _expenseDao.insertExpense(entity);
  }

  @override
  Future<void> addExpenses(List<Expense> expenses) async {
    final entities = expenses.map((expense) => ExpensesCompanion.insert(
      date: expense.date,
      itemName: expense.itemName,
      quantity: expense.quantity,
      originalPrice: expense.amount,
      finalPrice: expense.amount,
      categoryId: expense.categoryId,
      status: 'Paid',
      isRecurring: expense.isRecurring,
      isInstallment: Value(expense.isInstallment),
      createdAt: DateTime.now().millisecondsSinceEpoch,
      updatedAt: DateTime.now().millisecondsSinceEpoch,
      platform: const Value(null),
      merchant: Value(expense.merchant),
    )).toList();
    
    // We need to implement insertAll in ExpenseDao if it's not there
    // For now, let's use a loop or just add insertAll to ExpenseDao
    await _expenseDao.batch((batch) {
      batch.insertAll(_expenseDao.expenses, entities, mode: InsertMode.replace);
    });
  }

  @override
  Future<void> updateExpense(Expense expense) async {
    final entity = ExpensesCompanion(
      id: Value(expense.id),
      date: Value(expense.date),
      itemName: Value(expense.itemName),
      quantity: Value(expense.quantity),
      originalPrice: Value(expense.amount),
      finalPrice: Value(expense.amount),
      categoryId: Value(expense.categoryId),
      isRecurring: Value(expense.isRecurring),
      isInstallment: Value(expense.isInstallment),
      updatedAt: Value(DateTime.now().millisecondsSinceEpoch),
      merchant: Value(expense.merchant),
    );
    await _expenseDao.updateExpense(entity);
  }

  @override
  Future<void> deleteExpense(Expense expense) async {
    final entity = ExpensesCompanion(id: Value(expense.id));
    await _expenseDao.deleteExpense(entity);
  }

  @override
  Future<List<String>> getItemNameSuggestions(String query) {
    return _expenseDao.getItemNameSuggestions(query);
  }

  @override
  Future<List<String>> getMerchantSuggestions(String query) {
    return _expenseDao.getMerchantSuggestions(query);
  }

  @override
  Stream<List<String>> getAllTags() {
    return _expenseDao.getAllTags();
  }

  @override
  Stream<List<Expense>> getFilteredExpenses({
    String? query,
    List<int>? categoryIds,
    int? since,
    int? until,
    int? minAmount,
    int? maxAmount,
    List<String>? tags,
  }) {
    return _expenseDao.getFilteredExpenses(
      query: query,
      categoryIds: categoryIds,
      since: since,
      until: until,
      minAmount: minAmount,
      maxAmount: maxAmount,
      tags: tags,
    ).map((items) => items.map((e) => Expense(
      id: e.expense.id,
      date: e.expense.date,
      itemName: e.expense.itemName,
      amount: e.expense.finalPrice,
      categoryId: e.expense.categoryId,
      isRecurring: e.expense.isRecurring,
      isInstallment: e.expense.isInstallment,
      merchant: e.expense.merchant,
      quantity: e.expense.quantity,
      tags: e.tags.map((t) => t.name).toList(),
    )).toList());
  }

  @override
  Stream<Map<int, int>> getDailySpendingBetween(int since, int until) {
    return _expenseDao.getDailySpendingBetween(since, until);
  }

  @override
  Stream<int?> getTotalSpentBetween(int since, int until) {
    return _expenseDao.getTotalSpentBetween(since, until);
  }

  @override
  Stream<List<CategorySpent>> getSpendingByCategoryBetween(int since, int until) {
    return _expenseDao.getSpendingByCategoryBetween(since, until);
  }

  @override
  Future<void> addCategory(Category category) async {
    await _categoryDao.insertCategory(CategoriesCompanion.insert(
      name: category.name,
      icon: category.icon,
      orderIndex: Value(category.orderIndex),
    ));
  }

  @override
  Future<void> updateCategory(Category category) async {
    await _categoryDao.updateCategory(CategoriesCompanion(
      id: Value(category.id),
      name: Value(category.name),
      icon: Value(category.icon),
      orderIndex: Value(category.orderIndex),
    ));
  }

  @override
  Future<void> deleteCategory(Category category) async {
    await _categoryDao.deleteCategory(CategoriesCompanion(id: Value(category.id)));
  }

  @override
  Future<void> updateCategories(List<Category> categories) async {
    final entities = categories.map((c) => CategoriesCompanion(
      id: Value(c.id),
      name: Value(c.name),
      icon: Value(c.icon),
      orderIndex: Value(c.orderIndex),
    )).toList();
    await _categoryDao.updateCategories(entities);
  }

  @override
  Stream<List<Tag>> getAllTagEntities() {
    return _tagDao.getAllTags().map((entities) => entities.map((e) => Tag(
      id: e.id,
      name: e.name,
      orderIndex: e.orderIndex,
    )).toList());
  }

  @override
  Future<void> updateTag(Tag tag) async {
    await _tagDao.updateTag(TagEntity(id: tag.id, name: tag.name, orderIndex: tag.orderIndex));
  }

  @override
  Future<void> deleteTag(Tag tag) async {
    await _tagDao.deleteTag(TagEntity(id: tag.id, name: tag.name, orderIndex: tag.orderIndex));
  }

  @override
  Future<void> updateTags(List<Tag> tags) async {
    final entities = tags.map((t) => TagEntity(id: t.id, name: t.name, orderIndex: t.orderIndex)).toList();
    await _tagDao.updateTags(entities);
  }
}
