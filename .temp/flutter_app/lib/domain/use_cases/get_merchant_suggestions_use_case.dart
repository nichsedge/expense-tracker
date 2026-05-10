import 'package:injectable/injectable.dart';
import '../repository/expense_repository.dart';

@lazySingleton
class GetMerchantSuggestionsUseCase {
  final ExpenseRepository _repository;

  GetMerchantSuggestionsUseCase(this._repository);

  Future<List<String>> call(String query) {
    return _repository.getMerchantSuggestions(query);
  }
}
