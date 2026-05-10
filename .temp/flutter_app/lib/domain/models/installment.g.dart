// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'installment.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Installment _$InstallmentFromJson(Map<String, dynamic> json) => Installment(
  id: (json['id'] as num?)?.toInt() ?? 0,
  expenseId: (json['expenseId'] as num).toInt(),
  totalAmount: (json['totalAmount'] as num).toInt(),
  monthlyPayment: (json['monthlyPayment'] as num).toInt(),
  durationMonths: (json['durationMonths'] as num).toInt(),
  remainingBalance: (json['remainingBalance'] as num).toInt(),
  nextDueDate: (json['nextDueDate'] as num).toInt(),
  status: json['status'] as String? ?? 'Active',
  createdAt: (json['createdAt'] as num).toInt(),
  expenseName: json['expenseName'] as String?,
  expenseDate: (json['expenseDate'] as num?)?.toInt(),
);

Map<String, dynamic> _$InstallmentToJson(Installment instance) =>
    <String, dynamic>{
      'id': instance.id,
      'expenseId': instance.expenseId,
      'totalAmount': instance.totalAmount,
      'monthlyPayment': instance.monthlyPayment,
      'durationMonths': instance.durationMonths,
      'remainingBalance': instance.remainingBalance,
      'nextDueDate': instance.nextDueDate,
      'status': instance.status,
      'createdAt': instance.createdAt,
      'expenseName': instance.expenseName,
      'expenseDate': instance.expenseDate,
    };
