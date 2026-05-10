import 'package:injectable/injectable.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class GetItemNameSuggestionsUseCase {
  final ExpenseRepository _repository;

  GetItemNameSuggestionsUseCase(this._repository);

  Future<List<String>> call(String query) {
    return _repository.getItemNameSuggestions(query);
  }
}
