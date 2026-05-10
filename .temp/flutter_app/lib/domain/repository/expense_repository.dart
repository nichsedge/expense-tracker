import '../models/expense.dart';
import '../models/category.dart';
import '../models/category_spent.dart';
import '../models/tag.dart';

abstract class ExpenseRepository {
  Stream<List<Expense>> getAllExpenses();
  Future<List<Expense>> getAllExpensesFuture();
  Stream<List<Category>> getAllCategories();
  Future<Expense?> getExpenseById(int id);
  Future<int> addExpense(Expense expense);
  Future<void> addExpenses(List<Expense> expenses);
  Future<void> updateExpense(Expense expense);
  Future<void> deleteExpense(Expense expense);
  Stream<List<String>> getAllTags();
  Future<List<String>> getItemNameSuggestions(String query);
  Future<List<String>> getMerchantSuggestions(String query);
  
  Stream<List<Expense>> getFilteredExpenses({
    String? query,
    List<int>? categoryIds,
    int? since,
    int? until,
    int? minAmount,
    int? maxAmount,
    List<String>? tags,
  });

  Stream<Map<int, int>> getDailySpendingBetween(int since, int until);
  Stream<int?> getTotalSpentBetween(int since, int until);
  Stream<List<CategorySpent>> getSpendingByCategoryBetween(int since, int until);

  // Category Management
  Future<void> addCategory(Category category);
  Future<void> updateCategory(Category category);
  Future<void> deleteCategory(Category category);
  Future<void> updateCategories(List<Category> categories);

  // Tag Management
  Stream<List<Tag>> getAllTagEntities();
  Future<void> updateTag(Tag tag);
  Future<void> deleteTag(Tag tag);
  Future<void> updateTags(List<Tag> tags);
}
