import 'package:equatable/equatable.dart';
import '../../../domain/models/category.dart';
import '../../../domain/models/tag.dart';

class SettingsState extends Equatable {
  final List<Category> categories;
  final List<Tag> tags;
  final int monthlyBudget;
  final String language;
  final bool isLoading;
  final String? error;
  final String? successMessage;

  const SettingsState({
    this.categories = const [],
    this.tags = const [],
    this.monthlyBudget = 0,
    this.language = 'en',
    this.isLoading = false,
    this.error,
    this.successMessage,
  });

  @override
  List<Object?> get props => [
    categories,
    tags,
    monthlyBudget,
    language,
    isLoading,
    error,
    successMessage,
  ];

  SettingsState copyWith({
    List<Category>? categories,
    List<Tag>? tags,
    int? monthlyBudget,
    String? language,
    bool? isLoading,
    String? error,
    String? successMessage,
  }) {
    return SettingsState(
      categories: categories ?? this.categories,
      tags: tags ?? this.tags,
      monthlyBudget: monthlyBudget ?? this.monthlyBudget,
      language: language ?? this.language,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      successMessage: successMessage,
    );
  }
}
