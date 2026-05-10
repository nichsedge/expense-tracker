import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'expense.g.dart';

@JsonSerializable()
class Expense extends Equatable {
  final int id;
  final int date;
  final String itemName;
  final int amount;
  final int categoryId;
  final bool isRecurring;
  final bool isInstallment;
  final bool isInstallmentPayment;
  final String? merchant;
  final List<String> tags;
  final int quantity;
  
  // Installment specific fields
  final int totalPaid;
  final int remainingBalance;
  final int monthlyPayment;

  const Expense({
    this.id = 0,
    required this.date,
    required this.itemName,
    required this.amount,
    required this.categoryId,
    this.isRecurring = false,
    this.isInstallment = false,
    this.isInstallmentPayment = false,
    this.merchant,
    this.tags = const [],
    this.quantity = 1,
    this.totalPaid = 0,
    this.remainingBalance = 0,
    this.monthlyPayment = 0,
  });

  factory Expense.fromJson(Map<String, dynamic> json) => _$ExpenseFromJson(json);
  Map<String, dynamic> toJson() => _$ExpenseToJson(this);

  @override
  List<Object?> get props => [
        id,
        date,
        itemName,
        amount,
        categoryId,
        isRecurring,
        isInstallment,
        isInstallmentPayment,
        merchant,
        tags,
        quantity,
        totalPaid,
        remainingBalance,
        monthlyPayment,
      ];

  Expense copyWith({
    int? id,
    int? date,
    String? itemName,
    int? amount,
    int? categoryId,
    bool? isRecurring,
    bool? isInstallment,
    bool? isInstallmentPayment,
    String? merchant,
    List<String>? tags,
    int? quantity,
    int? totalPaid,
    int? remainingBalance,
    int? monthlyPayment,
  }) {
    return Expense(
      id: id ?? this.id,
      date: date ?? this.date,
      itemName: itemName ?? this.itemName,
      amount: amount ?? this.amount,
      categoryId: categoryId ?? this.categoryId,
      isRecurring: isRecurring ?? this.isRecurring,
      isInstallment: isInstallment ?? this.isInstallment,
      isInstallmentPayment: isInstallmentPayment ?? this.isInstallmentPayment,
      merchant: merchant ?? this.merchant,
      tags: tags ?? this.tags,
      quantity: quantity ?? this.quantity,
      totalPaid: totalPaid ?? this.totalPaid,
      remainingBalance: remainingBalance ?? this.remainingBalance,
      monthlyPayment: monthlyPayment ?? this.monthlyPayment,
    );
  }
}
