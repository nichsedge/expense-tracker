import 'package:equatable/equatable.dart';

class CategorySpent extends Equatable {
  final int categoryId;
  final String categoryName;
  final String categoryIcon;
  final int totalAmount;

  const CategorySpent({
    required this.categoryId,
    required this.categoryName,
    required this.categoryIcon,
    required this.totalAmount,
  });

  @override
  List<Object?> get props => [categoryId, categoryName, categoryIcon, totalAmount];
}
