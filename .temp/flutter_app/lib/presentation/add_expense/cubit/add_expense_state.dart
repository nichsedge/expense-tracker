import 'package:equatable/equatable.dart';
import '../../../domain/models/category.dart';

class AddExpenseState extends Equatable {
  final String amount;
  final String itemName;
  final String merchant;
  final int categoryId;
  final bool isInstallment;
  final String durationMonths;
  final int selectedDate;
  final List<String> selectedTags;
  final List<String> itemNameSuggestions;
  final List<String> merchantSuggestions;
  final List<Category> categories;
  final List<String> allTags;
  final String newTagText;
  final bool isLoading;
  final bool isSaved;
  final String? error;

  const AddExpenseState({
    this.amount = "",
    this.itemName = "",
    this.merchant = "",
    this.categoryId = 1,
    this.isInstallment = false,
    this.durationMonths = "",
    this.selectedDate = 0,
    this.selectedTags = const [],
    this.itemNameSuggestions = const [],
    this.merchantSuggestions = const [],
    this.categories = const [],
    this.allTags = const [],
    this.newTagText = "",
    this.isLoading = false,
    this.isSaved = false,
    this.error,
  });

  AddExpenseState copyWith({
    String? amount,
    String? itemName,
    String? merchant,
    int? categoryId,
    bool? isInstallment,
    String? durationMonths,
    int? selectedDate,
    List<String>? selectedTags,
    List<String>? itemNameSuggestions,
    List<String>? merchantSuggestions,
    List<Category>? categories,
    List<String>? allTags,
    String? newTagText,
    bool? isLoading,
    bool? isSaved,
    String? error,
  }) {
    return AddExpenseState(
      amount: amount ?? this.amount,
      itemName: itemName ?? this.itemName,
      merchant: merchant ?? this.merchant,
      categoryId: categoryId ?? this.categoryId,
      isInstallment: isInstallment ?? this.isInstallment,
      durationMonths: durationMonths ?? this.durationMonths,
      selectedDate: selectedDate ?? this.selectedDate,
      selectedTags: selectedTags ?? this.selectedTags,
      itemNameSuggestions: itemNameSuggestions ?? this.itemNameSuggestions,
      merchantSuggestions: merchantSuggestions ?? this.merchantSuggestions,
      categories: categories ?? this.categories,
      allTags: allTags ?? this.allTags,
      newTagText: newTagText ?? this.newTagText,
      isLoading: isLoading ?? this.isLoading,
      isSaved: isSaved ?? this.isSaved,
      error: error ?? this.error,
    );
  }

  @override
  List<Object?> get props => [
        amount,
        itemName,
        merchant,
        categoryId,
        isInstallment,
        durationMonths,
        selectedDate,
        selectedTags,
        itemNameSuggestions,
        merchantSuggestions,
        categories,
        allTags,
        newTagText,
        isLoading,
        isSaved,
        error,
      ];
}
