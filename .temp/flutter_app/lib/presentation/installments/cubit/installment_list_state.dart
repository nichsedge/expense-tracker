import 'package:equatable/equatable.dart';
import '../../../domain/models/installment.dart';

class InstallmentListState extends Equatable {
  final List<Installment> activeInstallments;
  final List<Installment> historyInstallments;
  final int totalMonthlyDue;
  final int totalRemainingBalance;
  final int selectedTab;
  final bool isLoading;
  final String? error;

  const InstallmentListState({
    this.activeInstallments = const [],
    this.historyInstallments = const [],
    this.totalMonthlyDue = 0,
    this.totalRemainingBalance = 0,
    this.selectedTab = 0,
    this.isLoading = false,
    this.error,
  });

  @override
  List<Object?> get props => [
    activeInstallments,
    historyInstallments,
    totalMonthlyDue,
    totalRemainingBalance,
    selectedTab,
    isLoading,
    error,
  ];

  InstallmentListState copyWith({
    List<Installment>? activeInstallments,
    List<Installment>? historyInstallments,
    int? totalMonthlyDue,
    int? totalRemainingBalance,
    int? selectedTab,
    bool? isLoading,
    String? error,
  }) {
    return InstallmentListState(
      activeInstallments: activeInstallments ?? this.activeInstallments,
      historyInstallments: historyInstallments ?? this.historyInstallments,
      totalMonthlyDue: totalMonthlyDue ?? this.totalMonthlyDue,
      totalRemainingBalance: totalRemainingBalance ?? this.totalRemainingBalance,
      selectedTab: selectedTab ?? this.selectedTab,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}
