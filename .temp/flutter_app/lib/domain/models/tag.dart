import 'package:equatable/equatable.dart';

class Tag extends Equatable {
  final int id;
  final String name;
  final int orderIndex;

  const Tag({
    required this.id,
    required this.name,
    this.orderIndex = 0,
  });

  @override
  List<Object?> get props => [id, name, orderIndex];

  Tag copyWith({
    int? id,
    String? name,
    int? orderIndex,
  }) {
    return Tag(
      id: id ?? this.id,
      name: name ?? this.name,
      orderIndex: orderIndex ?? this.orderIndex,
    );
  }
}
