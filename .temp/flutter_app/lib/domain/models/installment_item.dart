import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'installment_item.g.dart';

@JsonSerializable()
class InstallmentItem extends Equatable {
  final int id;
  final int installmentId;
  final int amount;
  final int dueDate;
  final String status;
  final int monthNumber;

  const InstallmentItem({
    required this.id,
    required this.installmentId,
    required this.amount,
    required this.dueDate,
    required this.status,
    required this.monthNumber,
  });

  factory InstallmentItem.fromJson(Map<String, dynamic> json) => _$InstallmentItemFromJson(json);
  Map<String, dynamic> toJson() => _$InstallmentItemToJson(this);

  @override
  List<Object?> get props => [id, installmentId, amount, dueDate, status, monthNumber];
}
