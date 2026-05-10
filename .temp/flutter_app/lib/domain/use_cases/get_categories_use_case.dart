import 'package:injectable/injectable.dart';
import '../models/category.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class GetCategoriesUseCase {
  final ExpenseRepository _repository;

  GetCategoriesUseCase(this._repository);

  Stream<List<Category>> call() {
    return _repository.getAllCategories();
  }
}
