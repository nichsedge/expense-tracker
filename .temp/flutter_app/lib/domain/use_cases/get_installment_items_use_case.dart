import 'package:injectable/injectable.dart';
import '../models/installment_item.dart';
import '../repository/installment_repository.dart';

@lazySingleton
class GetInstallmentItemsUseCase {
  final InstallmentRepository _repository;

  GetInstallmentItemsUseCase(this._repository);

  Stream<List<InstallmentItem>> call(int installmentId) {
    return _repository.getItemsForInstallment(installmentId);
  }
}
