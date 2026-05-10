import 'package:injectable/injectable.dart';
import '../models/installment.dart';
import '../repository/installment_repository.dart';

@lazySingleton
class GetInstallmentsUseCase {
  final InstallmentRepository _repository;

  GetInstallmentsUseCase(this._repository);

  Stream<List<Installment>> call() {
    return _repository.getAllInstallments();
  }
}
