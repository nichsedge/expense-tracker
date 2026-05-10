import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'installment.g.dart';

@JsonSerializable()
class Installment extends Equatable {
  final int id;
  final int expenseId;
  final int totalAmount;
  final int monthlyPayment;
  final int durationMonths;
  final int remainingBalance;
  final int nextDueDate;
  final String status;
  final int createdAt;
  final String? expenseName;
  final int? expenseDate;

  const Installment({
    this.id = 0,
    required this.expenseId,
    required this.totalAmount,
    required this.monthlyPayment,
    required this.durationMonths,
    required this.remainingBalance,
    required this.nextDueDate,
    this.status = 'Active',
    required this.createdAt,
    this.expenseName,
    this.expenseDate,
  });

  factory Installment.fromJson(Map<String, dynamic> json) => _$InstallmentFromJson(json);
  Map<String, dynamic> toJson() => _$InstallmentToJson(this);

  @override
  List<Object?> get props => [
        id,
        expenseId,
        totalAmount,
        monthlyPayment,
        durationMonths,
        remainingBalance,
        nextDueDate,
        status,
        createdAt,
        expenseName,
        expenseDate,
      ];

  Installment copyWith({
    int? id,
    int? expenseId,
    int? totalAmount,
    int? monthlyPayment,
    int? durationMonths,
    int? remainingBalance,
    int? nextDueDate,
    String? status,
    int? createdAt,
    String? expenseName,
    int? expenseDate,
  }) {
    return Installment(
      id: id ?? this.id,
      expenseId: expenseId ?? this.expenseId,
      totalAmount: totalAmount ?? this.totalAmount,
      monthlyPayment: monthlyPayment ?? this.monthlyPayment,
      durationMonths: durationMonths ?? this.durationMonths,
      remainingBalance: remainingBalance ?? this.remainingBalance,
      nextDueDate: nextDueDate ?? this.nextDueDate,
      status: status ?? this.status,
      createdAt: createdAt ?? this.createdAt,
      expenseName: expenseName ?? this.expenseName,
      expenseDate: expenseDate ?? this.expenseDate,
    );
  }
}
