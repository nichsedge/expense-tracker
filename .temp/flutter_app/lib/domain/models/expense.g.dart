// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'expense.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Expense _$ExpenseFromJson(Map<String, dynamic> json) => Expense(
  id: (json['id'] as num?)?.toInt() ?? 0,
  date: (json['date'] as num).toInt(),
  itemName: json['itemName'] as String,
  amount: (json['amount'] as num).toInt(),
  categoryId: (json['categoryId'] as num).toInt(),
  isRecurring: json['isRecurring'] as bool? ?? false,
  isInstallment: json['isInstallment'] as bool? ?? false,
  isInstallmentPayment: json['isInstallmentPayment'] as bool? ?? false,
  merchant: json['merchant'] as String?,
  tags:
      (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList() ??
      const [],
  quantity: (json['quantity'] as num?)?.toInt() ?? 1,
  totalPaid: (json['totalPaid'] as num?)?.toInt() ?? 0,
  remainingBalance: (json['remainingBalance'] as num?)?.toInt() ?? 0,
  monthlyPayment: (json['monthlyPayment'] as num?)?.toInt() ?? 0,
);

Map<String, dynamic> _$ExpenseToJson(Expense instance) => <String, dynamic>{
  'id': instance.id,
  'date': instance.date,
  'itemName': instance.itemName,
  'amount': instance.amount,
  'categoryId': instance.categoryId,
  'isRecurring': instance.isRecurring,
  'isInstallment': instance.isInstallment,
  'isInstallmentPayment': instance.isInstallmentPayment,
  'merchant': instance.merchant,
  'tags': instance.tags,
  'quantity': instance.quantity,
  'totalPaid': instance.totalPaid,
  'remainingBalance': instance.remainingBalance,
  'monthlyPayment': instance.monthlyPayment,
};
