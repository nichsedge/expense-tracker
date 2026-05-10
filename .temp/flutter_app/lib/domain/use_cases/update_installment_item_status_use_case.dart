import 'package:injectable/injectable.dart';
import '../repository/installment_repository.dart';

@lazySingleton
class UpdateInstallmentItemStatusUseCase {
  final InstallmentRepository _repository;

  UpdateInstallmentItemStatusUseCase(this._repository);

  Future<void> call(int itemId, String status) {
    return _repository.updateInstallmentItemStatus(itemId, status);
  }
}
