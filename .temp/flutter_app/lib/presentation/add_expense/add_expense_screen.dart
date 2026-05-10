import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../core/util/date_formatter_utils.dart';
import '../../di/injection.dart';
import 'cubit/add_expense_cubit.dart';
import 'cubit/add_expense_state.dart';

class AddExpenseScreen extends StatelessWidget {
  final int? expenseId;
  const AddExpenseScreen({super.key, this.expenseId});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) {
        final cubit = getIt<AddExpenseCubit>();
        if (expenseId != null) {
          cubit.loadExpense(expenseId!);
        }
        return cubit;
      },
      child: const AddExpenseView(),
    );
  }
}

class AddExpenseView extends StatelessWidget {
  const AddExpenseView({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocListener<AddExpenseCubit, AddExpenseState>(
      listener: (context, state) {
        if (state.isSaved) {
          context.pop();
        }
        if (state.error != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.error!)),
          );
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: BlocBuilder<AddExpenseCubit, AddExpenseState>(
            builder: (context, state) => Text(
              state.isSaved ? 'Edit Expense' : 'Add Expense' // Fix logic later
            ),
          ),
          actions: [
            BlocBuilder<AddExpenseCubit, AddExpenseState>(
              builder: (context, state) {
                return state.isLoading
                    ? const Padding(
                        padding: EdgeInsets.all(16),
                        child: CircularProgressIndicator(),
                      )
                    : TextButton(
                        onPressed: () => context.read<AddExpenseCubit>().save(),
                        child: const Text('SAVE'),
                      );
              },
            ),
          ],
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const _AmountField(),
            const SizedBox(height: 16),
            const _ItemNameField(),
            const SizedBox(height: 16),
            const _MerchantField(),
            const SizedBox(height: 16),
            const _CategoryPicker(),
            const SizedBox(height: 16),
            const _DatePicker(),
            const SizedBox(height: 16),
            const _InstallmentSwitch(),
            const _TagSection(),
          ],
        ),
      ),
    );
  }
}

class _AmountField extends StatelessWidget {
  const _AmountField();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return TextField(
          decoration: const InputDecoration(
            labelText: 'Amount',
            prefixIcon: Icon(Icons.attach_money),
            border: OutlineInputBorder(),
          ),
          keyboardType: TextInputType.number,
          onChanged: (value) => context.read<AddExpenseCubit>().onAmountChanged(value),
        );
      },
    );
  }
}

class _ItemNameField extends StatelessWidget {
  const _ItemNameField();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return Column(
          children: [
            TextField(
              decoration: const InputDecoration(
                labelText: 'What did you buy?',
                prefixIcon: Icon(Icons.shopping_cart),
                border: OutlineInputBorder(),
              ),
              onChanged: (value) => context.read<AddExpenseCubit>().onItemNameChanged(value),
            ),
            if (state.itemNameSuggestions.isNotEmpty)
              _SuggestionsList(
                suggestions: state.itemNameSuggestions,
                onSelected: (value) => context.read<AddExpenseCubit>().onItemNameChanged(value),
              ),
          ],
        );
      },
    );
  }
}

class _MerchantField extends StatelessWidget {
  const _MerchantField();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return Column(
          children: [
            TextField(
              decoration: const InputDecoration(
                labelText: 'Merchant',
                prefixIcon: Icon(Icons.store),
                border: OutlineInputBorder(),
              ),
              onChanged: (value) => context.read<AddExpenseCubit>().onMerchantChanged(value),
            ),
            if (state.merchantSuggestions.isNotEmpty)
              _SuggestionsList(
                suggestions: state.merchantSuggestions,
                onSelected: (value) => context.read<AddExpenseCubit>().onMerchantChanged(value),
              ),
          ],
        );
      },
    );
  }
}

class _SuggestionsList extends StatelessWidget {
  final List<String> suggestions;
  final Function(String) onSelected;

  const _SuggestionsList({required this.suggestions, required this.onSelected});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 4),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        children: suggestions.map((s) => ListTile(
          title: Text(s),
          onTap: () => onSelected(s),
        )).toList(),
      ),
    );
  }
}

class _CategoryPicker extends StatelessWidget {
  const _CategoryPicker();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Category', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: state.categories.map((cat) {
                final isSelected = state.categoryId == cat.id;
                return ChoiceChip(
                  label: Text(cat.name),
                  selected: isSelected,
                  onSelected: (_) => context.read<AddExpenseCubit>().onCategoryChanged(cat.id),
                );
              }).toList(),
            ),
          ],
        );
      },
    );
  }
}

class _DatePicker extends StatelessWidget {
  const _DatePicker();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return ListTile(
          title: const Text('Transaction Date'),
          subtitle: Text(DateFormatterUtils.formatDate(state.selectedDate)),
          leading: const Icon(Icons.calendar_today),
          onTap: () async {
            final date = await showDatePicker(
              context: context,
              initialDate: DateTime.fromMillisecondsSinceEpoch(state.selectedDate),
              firstDate: DateTime(2000),
              lastDate: DateTime(2100),
            );
            if (date != null && context.mounted) {
              context.read<AddExpenseCubit>().onDateChanged(date.millisecondsSinceEpoch);
            }
          },
        );
      },
    );
  }
}

class _InstallmentSwitch extends StatelessWidget {
  const _InstallmentSwitch();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return Column(
          children: [
            SwitchListTile(
              title: const Text('Is Installment?'),
              value: state.isInstallment,
              onChanged: (value) => context.read<AddExpenseCubit>().toggleInstallment(value),
            ),
            if (state.isInstallment)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: TextField(
                  decoration: const InputDecoration(
                    labelText: 'Duration (Months)',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                  onChanged: (value) => context.read<AddExpenseCubit>().onDurationChanged(value),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _TagSection extends StatelessWidget {
  const _TagSection();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AddExpenseCubit, AddExpenseState>(
      builder: (context, state) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 16),
            const Text('Tags', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                ...state.selectedTags.map((tag) => Chip(
                  label: Text(tag),
                  onDeleted: () => context.read<AddExpenseCubit>().toggleTag(tag),
                )),
                ActionChip(
                  label: const Icon(Icons.add, size: 16),
                  onPressed: () => _showAddTagDialog(context),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  void _showAddTagDialog(BuildContext context) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (dContext) => AlertDialog(
        title: const Text('Add Tag'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(labelText: 'Tag Name'),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dContext), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                context.read<AddExpenseCubit>().toggleTag(controller.text.trim().toLowerCase());
                Navigator.pop(dContext);
              }
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
}
