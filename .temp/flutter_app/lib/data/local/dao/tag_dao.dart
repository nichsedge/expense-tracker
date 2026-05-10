import 'package:drift/drift.dart';
import '../database.dart';

part 'tag_dao.g.dart';

@DriftAccessor(tables: [Tags])
class TagDao extends DatabaseAccessor<AppDatabase> with _$TagDaoMixin {
  TagDao(super.db);

  Stream<List<TagEntity>> getAllTags() {
    return (select(tags)
          ..orderBy([
            (t) => OrderingTerm(expression: t.orderIndex, mode: OrderingMode.asc),
            (t) => OrderingTerm(expression: t.id, mode: OrderingMode.asc),
          ]))
        .watch();
  }

  Future<int> insertTag(Insertable<TagEntity> tag) {
    return into(tags).insert(tag, mode: InsertMode.replace);
  }

  Future<TagEntity?> getTagByName(String name) {
    return (select(tags)..where((t) => t.name.equals(name))).getSingleOrNull();
  }

  Future<bool> updateTag(Insertable<TagEntity> tag) {
    return update(tags).replace(tag);
  }

  Future<void> updateTags(List<Insertable<TagEntity>> tagList) async {
    await batch((batch) {
      batch.replaceAll(tags, tagList);
    });
  }

  Future<int> deleteTag(Insertable<TagEntity> tag) {
    return delete(tags).delete(tag);
  }
}
