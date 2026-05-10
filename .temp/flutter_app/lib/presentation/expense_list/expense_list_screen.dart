import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import '../../core/util/currency_formatter.dart';
import '../../di/injection.dart';
import '../../domain/models/expense.dart';
import 'cubit/expense_list_cubit.dart';
import 'cubit/expense_list_state.dart';

class ExpenseListScreen extends StatelessWidget {
  const ExpenseListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => getIt<ExpenseListCubit>(),
      child: const ExpenseListView(),
    );
  }
}

class ExpenseListView extends StatelessWidget {
  const ExpenseListView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Expenses'),
        actions: [
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: 'Import Database',
            onPressed: () => _importDatabase(context),
          ),
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: () => _showFilterSheet(context),
          ),
        ],
      ),
      drawer: const _NavigationDrawer(),
      body: Column(
        children: [
          const _BudgetOverview(),
          const _DateRangePicker(),
          const _SearchBar(),
          Expanded(
            child: BlocBuilder<ExpenseListCubit, ExpenseListState>(
              builder: (context, state) {
                if (state.isLoading) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (state.groupedExpenses.isEmpty) {
                  return const Center(child: Text('No expenses found'));
                }
                return ListView.builder(
                  itemCount: state.groupedExpenses.length,
                  itemBuilder: (context, index) {
                    final key = state.groupedExpenses.keys.elementAt(index);
                    final items = state.groupedExpenses[key]!;
                    return _DateGroup(dateKey: key, expenses: items);
                  },
                );
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          context.push('/add-expense');
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  void _showFilterSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (dContext) => BlocProvider.value(
        value: context.read<ExpenseListCubit>(),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Filters', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  TextButton(
                    onPressed: () {
                      context.read<ExpenseListCubit>().clearFilters();
                      Navigator.pop(dContext);
                    },
                    child: const Text('Clear All'),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Text('Categories', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              BlocBuilder<ExpenseListCubit, ExpenseListState>(
                builder: (context, state) {
                  return Wrap(
                    spacing: 8,
                    children: state.categories.map((cat) {
                      final isSelected = state.selectedCategoryIds.contains(cat.id);
                      return FilterChip(
                        label: Text(cat.name),
                        selected: isSelected,
                        onSelected: (_) => context.read<ExpenseListCubit>().toggleCategoryFilter(cat.id),
                      );
                    }).toList(),
                  );
                },
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

class _BudgetOverview extends StatelessWidget {
  const _BudgetOverview();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ExpenseListCubit, ExpenseListState>(
      builder: (context, state) {
        final remaining = state.monthlyBudget - state.thisMonthSpent;
        final percent = state.monthlyBudget > 0 
            ? (state.thisMonthSpent / state.monthlyBudget).clamp(0.0, 1.0)
            : 0.0;

        return Container(
          padding: const EdgeInsets.all(16),
          margin: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primaryContainer,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('This Month', style: TextStyle(fontWeight: FontWeight.bold)),
                  Text(CurrencyFormatter.formatAmount(state.thisMonthSpent)),
                ],
              ),
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: percent,
                backgroundColor: Colors.white24,
                color: percent > 0.9 ? Colors.red : null,
              ),
              const SizedBox(height: 8),
              Text(
                remaining >= 0 
                    ? '${CurrencyFormatter.formatAmount(remaining)} remaining'
                    : '${CurrencyFormatter.formatAmount(remaining.abs())} over budget',
                style: TextStyle(
                  fontSize: 12,
                  color: remaining < 0 ? Colors.red : null,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _DateRangePicker extends StatelessWidget {
  const _DateRangePicker();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ExpenseListCubit, ExpenseListState>(
      builder: (context, state) {
        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: DateRangeFilter.values.map((filter) {
              final isSelected = state.activeDateFilter == filter;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(_filterLabel(filter)),
                  selected: isSelected,
                  onSelected: (_) => context.read<ExpenseListCubit>().updateDateRange(filter),
                ),
              );
            }).toList(),
          ),
        );
      },
    );
  }

  String _filterLabel(DateRangeFilter filter) {
    switch (filter) {
      case DateRangeFilter.sevenDays: return '7 Days';
      case DateRangeFilter.thirtyDays: return '30 Days';
      case DateRangeFilter.thisMonth: return 'This Month';
      case DateRangeFilter.allTime: return 'All Time';
      case DateRangeFilter.custom: return 'Custom';
    }
  }
}

class _SearchBar extends StatelessWidget {
  const _SearchBar();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: SearchBar(
        hintText: 'Search expenses...',
        leading: const Icon(Icons.search),
        onChanged: (value) => context.read<ExpenseListCubit>().updateSearchQuery(value),
      ),
    );
  }
}

class _DateGroup extends StatelessWidget {
  final String dateKey;
  final List<Expense> expenses;

  const _DateGroup({required this.dateKey, required this.expenses});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Text(
            dateKey,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        ...expenses.map((expense) => _ExpenseItem(expense: expense)),
      ],
    );
  }
}

class _ExpenseItem extends StatelessWidget {
  final Expense expense;

  const _ExpenseItem({required this.expense});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: CircleAvatar(
        child: Icon(_getCategoryIcon(expense.categoryId)),
      ),
      title: Text(expense.itemName),
      subtitle: Text(expense.merchant ?? 'Unknown Merchant'),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            CurrencyFormatter.formatAmount(expense.amount),
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          if (expense.isInstallment)
            const Text('Installment', style: TextStyle(fontSize: 10, color: Colors.teal)),
        ],
      ),
      onTap: () {
        context.push('/edit-expense/${expense.id}');
      },
    );
  }

  IconData _getCategoryIcon(int categoryId) {
    // Simplified icon mapping
    switch (categoryId) {
      case 1: return Icons.fastfood;
      case 2: return Icons.directions_car;
      case 3: return Icons.shopping_bag;
      case 4: return Icons.movie;
      case 5: return Icons.receipt_long;
      default: return Icons.category;
    }
  }
}

Future<void> _importDatabase(BuildContext context) async {
  try {
    FilePickerResult? result = await FilePicker.platform.pickFiles(type: FileType.any);

    if (result != null && result.files.single.path != null) {
      final sourceFile = File(result.files.single.path!);
      final bytes = await sourceFile.readAsBytes();
      
      final dbFolder = await getApplicationDocumentsDirectory();
      final targetPath = p.join(dbFolder.path, 'expense_tracker.sqlite');
      final targetFile = File(targetPath);
      
      await targetFile.writeAsBytes(bytes);
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Database imported! Please restart the app to see changes.')),
        );
      }
    }
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error importing: $e')),
      );
    }
  }
}

class _NavigationDrawer extends StatelessWidget {
  const _NavigationDrawer();

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          DrawerHeader(
            decoration: BoxDecoration(color: Theme.of(context).colorScheme.primary),
            child: const Text(
              'Expense Tracker',
              style: TextStyle(color: Colors.white, fontSize: 24),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.list),
            title: const Text('Expenses'),
            onTap: () => context.pop(),
          ),
          ListTile(
            leading: const Icon(Icons.calendar_month),
            title: const Text('Installments'),
            onTap: () {
              context.pop();
              context.push('/installments');
            },
          ),
          ListTile(
            leading: const Icon(Icons.bar_chart),
            title: const Text('Statistics'),
            onTap: () {
              context.pop();
              context.push('/stats');
            },
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.settings),
            title: const Text('Settings'),
            onTap: () {
              context.pop();
              context.push('/settings');
            },
          ),
        ],
      ),
    );
  }
}
