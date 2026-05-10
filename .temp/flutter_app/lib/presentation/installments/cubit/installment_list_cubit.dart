import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import 'package:rxdart/rxdart.dart';
import '../../../domain/use_cases/get_installments_use_case.dart';
import '../../../domain/use_cases/get_installment_items_use_case.dart';
import '../../../domain/use_cases/update_installment_item_status_use_case.dart';
import '../../../domain/repository/installment_repository.dart';
import '../../../domain/models/installment_item.dart';
import 'installment_list_state.dart';

@injectable
class InstallmentListCubit extends Cubit<InstallmentListState> {
  final GetInstallmentsUseCase _getInstallmentsUseCase;
  final GetInstallmentItemsUseCase _getInstallmentItemsUseCase;
  final UpdateInstallmentItemStatusUseCase _updateInstallmentItemStatusUseCase;
  final InstallmentRepository _repository;

  StreamSubscription? _installmentsSubscription;
  StreamSubscription? _statsSubscription;

  InstallmentListCubit(
    this._getInstallmentsUseCase,
    this._getInstallmentItemsUseCase,
    this._updateInstallmentItemStatusUseCase,
    this._repository,
  ) : super(const InstallmentListState()) {
    _init();
  }

  void _init() {
    _loadInstallments();
    _loadStats();
  }

  void _loadInstallments() {
    _installmentsSubscription?.cancel();
    emit(state.copyWith(isLoading: true));

    _installmentsSubscription = _getInstallmentsUseCase().listen((installments) {
      final active = installments.where((i) => i.status == 'Active').toList();
      final history = installments.where((i) => i.status == 'Completed').toList();
      
      emit(state.copyWith(
        activeInstallments: active,
        historyInstallments: history,
        isLoading: false,
      ));
    }, onError: (e) {
      emit(state.copyWith(isLoading: false, error: e.toString()));
    });
  }

  void _loadStats() {
    _statsSubscription?.cancel();
    
    final now = DateTime.now();
    final startOfMonth = DateTime(now.year, now.month, 1).millisecondsSinceEpoch;
    final endOfMonth = DateTime(now.year, now.month + 1, 1).millisecondsSinceEpoch;

    _statsSubscription = Rx.combineLatest2(
      _repository.getTotalMonthlyDue(startOfMonth, endOfMonth),
      _repository.getTotalRemainingBalance(),
      (monthlyDue, remaining) => {
        'monthlyDue': monthlyDue ?? 0,
        'remaining': remaining ?? 0,
      },
    ).listen((stats) {
      emit(state.copyWith(
        totalMonthlyDue: stats['monthlyDue'],
        totalRemainingBalance: stats['remaining'],
      ));
    });
  }

  void onTabSelected(int index) {
    emit(state.copyWith(selectedTab: index));
  }

  Stream<List<InstallmentItem>> getItemsForInstallment(int installmentId) {
    return _getInstallmentItemsUseCase(installmentId);
  }

  Future<void> toggleStatus(int itemId, String currentStatus) async {
    final newStatus = currentStatus == 'Paid' ? 'Pending' : 'Paid';
    await _updateInstallmentItemStatusUseCase(itemId, newStatus);
    // The stream in the UI will update automatically
  }

  @override
  Future<void> close() {
    _installmentsSubscription?.cancel();
    _statsSubscription?.cancel();
    return super.close();
  }
}
