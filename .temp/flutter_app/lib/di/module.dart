import 'package:injectable/injectable.dart';
import '../data/local/database.dart';
import '../data/local/dao/expense_dao.dart';
import '../data/local/dao/category_dao.dart';
import '../domain/repository/expense_repository.dart';
import '../data/repository/expense_repository_impl.dart';
import '../data/local/dao/installment_dao.dart';
import '../domain/repository/installment_repository.dart';
import '../data/repository/installment_repository_impl.dart';
import '../data/local/dao/tag_dao.dart';

@module
abstract class AppModule {
  @singleton
  AppDatabase get db => AppDatabase();

  @lazySingleton
  ExpenseDao getExpenseDao(AppDatabase db) => ExpenseDao(db);

  @lazySingleton
  CategoryDao getCategoryDao(AppDatabase db) => CategoryDao(db);

  @lazySingleton
  TagDao getTagDao(AppDatabase db) => TagDao(db);

  @lazySingleton
  ExpenseRepository getExpenseRepository(ExpenseDao expenseDao, CategoryDao categoryDao, TagDao tagDao) => 
      ExpenseRepositoryImpl(expenseDao, categoryDao, tagDao);

  @lazySingleton
  InstallmentDao getInstallmentDao(AppDatabase db) => InstallmentDao(db);

  @lazySingleton
  InstallmentRepository getInstallmentRepository(InstallmentDao installmentDao) => 
      InstallmentRepositoryImpl(installmentDao);
}
