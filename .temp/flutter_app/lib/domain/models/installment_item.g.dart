// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'installment_item.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

InstallmentItem _$InstallmentItemFromJson(Map<String, dynamic> json) =>
    InstallmentItem(
      id: (json['id'] as num).toInt(),
      installmentId: (json['installmentId'] as num).toInt(),
      amount: (json['amount'] as num).toInt(),
      dueDate: (json['dueDate'] as num).toInt(),
      status: json['status'] as String,
      monthNumber: (json['monthNumber'] as num).toInt(),
    );

Map<String, dynamic> _$InstallmentItemToJson(InstallmentItem instance) =>
    <String, dynamic>{
      'id': instance.id,
      'installmentId': instance.installmentId,
      'amount': instance.amount,
      'dueDate': instance.dueDate,
      'status': instance.status,
      'monthNumber': instance.monthNumber,
    };
