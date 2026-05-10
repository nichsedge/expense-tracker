import 'package:drift/drift.dart';
import '../database.dart';

part 'category_dao.g.dart';

@DriftAccessor(tables: [Categories])
class CategoryDao extends DatabaseAccessor<AppDatabase> with _$CategoryDaoMixin {
  CategoryDao(super.db);

  Stream<List<CategoryEntity>> getAllCategories() {
    return (select(categories)
          ..orderBy([
            (t) => OrderingTerm(expression: t.orderIndex, mode: OrderingMode.asc),
            (t) => OrderingTerm(expression: t.id, mode: OrderingMode.asc),
          ]))
        .watch();
  }

  Future<int> insertCategory(Insertable<CategoryEntity> category) {
    return into(categories).insert(category, mode: InsertMode.insertOrIgnore);
  }

  Future<bool> updateCategory(Insertable<CategoryEntity> category) {
    return update(categories).replace(category);
  }

  Future<void> updateCategories(List<Insertable<CategoryEntity>> categoryList) async {
    await batch((batch) {
      batch.replaceAll(categories, categoryList);
    });
  }

  Future<int> deleteCategory(Insertable<CategoryEntity> category) {
    return delete(categories).delete(category);
  }

  Future<CategoryEntity?> getCategoryById(int id) {
    return (select(categories)..where((t) => t.id.equals(id))).getSingleOrNull();
  }

  Future<int> getCount() async {
    final countExp = categories.id.count();
    final query = selectOnly(categories)..addColumns([countExp]);
    final result = await query.getSingle();
    return result.read(countExp) ?? 0;
  }
}
