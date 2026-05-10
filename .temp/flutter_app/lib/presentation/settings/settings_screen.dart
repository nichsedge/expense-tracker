import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:file_picker/file_picker.dart';
import '../../core/util/currency_formatter.dart';
import '../../di/injection.dart';
import '../../domain/models/category.dart';
import '../../domain/models/tag.dart';
import 'cubit/settings_cubit.dart';
import 'cubit/settings_state.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => getIt<SettingsCubit>(),
      child: const SettingsView(),
    );
  }
}

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  void _showCategoryDialog(BuildContext context, [Category? category]) {
    final nameController = TextEditingController(text: category?.name);
    String selectedIcon = category?.icon ?? '🍔';

    showDialog(
      context: context,
      builder: (dContext) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: Text(category == null ? 'Add Category' : 'Edit Category'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(labelText: 'Name'),
              ),
              const SizedBox(height: 16),
              const Text('Select Icon'),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: ['🍔', '💊', '🛍️', '🚗', '🌐', '🏠', '🎮', '🎁'].map((icon) {
                  return ChoiceChip(
                    label: Text(icon, style: const TextStyle(fontSize: 20)),
                    selected: selectedIcon == icon,
                    onSelected: (selected) {
                      if (selected) setState(() => selectedIcon = icon);
                    },
                  );
                }).toList(),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dContext), child: const Text('Cancel')),
            TextButton(
              onPressed: () {
                if (nameController.text.isNotEmpty) {
                  if (category == null) {
                    context.read<SettingsCubit>().addCategory(nameController.text, selectedIcon);
                  } else {
                    context.read<SettingsCubit>().updateCategory(category.copyWith(
                      name: nameController.text,
                      icon: selectedIcon,
                    ));
                  }
                  Navigator.pop(dContext);
                }
              },
              child: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocBuilder<SettingsCubit, SettingsState>(
        builder: (context, state) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const _SectionHeader(title: 'Features'),
              _BudgetTile(budget: state.monthlyBudget),
              const Divider(),
              const _SectionHeader(title: 'Data Management'),
              ListTile(
                leading: const Icon(Icons.upload),
                title: const Text('Export to CSV'),
                onTap: () => context.read<SettingsCubit>().exportToCsv(),
              ),
              ListTile(
                leading: const Icon(Icons.download),
                title: const Text('Import from CSV'),
                onTap: () async {
                  final result = await FilePicker.platform.pickFiles(
                    type: FileType.custom,
                    allowedExtensions: ['csv'],
                  );
                  if (result != null && result.files.single.path != null) {
                    if (context.mounted) {
                      context.read<SettingsCubit>().importFromCsv(result.files.single.path!);
                    }
                  }
                },
              ),
              const Divider(),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const _SectionHeader(title: 'Categories'),
                  TextButton.icon(
                    onPressed: () => _showCategoryDialog(context),
                    icon: const Icon(Icons.add),
                    label: const Text('Add'),
                  ),
                ],
              ),
              ...state.categories.asMap().entries.map((e) => _CategoryItem(
                category: e.value,
                index: e.key,
                total: state.categories.length,
                onEdit: () => _showCategoryDialog(context, e.value),
              )),
              const Divider(),
              const _SectionHeader(title: 'Tags'),
              if (state.tags.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: Text('No tags found. Tags are created when you add them to expenses.', 
                    style: TextStyle(color: Colors.grey, fontSize: 12)),
                ),
              ...state.tags.asMap().entries.map((e) => _TagItem(
                tag: e.value,
                index: e.key,
                total: state.tags.length,
              )),
            ],
          );
        },
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Text(
        title.toUpperCase(),
        style: TextStyle(
          color: Theme.of(context).colorScheme.primary,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}

class _BudgetTile extends StatelessWidget {
  final int budget;
  const _BudgetTile({required this.budget});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.shopping_cart),
      title: const Text('Monthly Budget'),
      subtitle: Text(budget > 0 ? CurrencyFormatter.formatAmount(budget) : 'Not Set'),
      onTap: () => _showBudgetDialog(context),
    );
  }

  void _showBudgetDialog(BuildContext context) {
    final controller = TextEditingController(text: budget > 0 ? (budget / 100).toStringAsFixed(0) : '');
    showDialog(
      context: context,
      builder: (dContext) => AlertDialog(
        title: const Text('Set Monthly Budget'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Amount'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dContext), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              final amount = int.tryParse(controller.text) ?? 0;
              context.read<SettingsCubit>().updateMonthlyBudget(amount * 100);
              Navigator.pop(dContext);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }
}

class _CategoryItem extends StatelessWidget {
  final Category category;
  final int index;
  final int total;
  final VoidCallback onEdit;

  const _CategoryItem({
    required this.category, 
    required this.index, 
    required this.total,
    required this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Text(category.icon, style: const TextStyle(fontSize: 24)),
      title: Text(category.name),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (index > 0)
            IconButton(
              icon: const Icon(Icons.arrow_upward, size: 20),
              onPressed: () {
                final cubit = context.read<SettingsCubit>();
                final list = List<Category>.from(cubit.state.categories);
                final item = list.removeAt(index);
                list.insert(index - 1, item);
                cubit.reorderCategories(list);
              },
            ),
          if (index < total - 1)
            IconButton(
              icon: const Icon(Icons.arrow_downward, size: 20),
              onPressed: () {
                final cubit = context.read<SettingsCubit>();
                final list = List<Category>.from(cubit.state.categories);
                final item = list.removeAt(index);
                list.insert(index + 1, item);
                cubit.reorderCategories(list);
              },
            ),
          IconButton(
            icon: const Icon(Icons.edit, size: 20),
            onPressed: onEdit,
          ),
        ],
      ),
    );
  }
}

class _TagItem extends StatelessWidget {
  final Tag tag;
  final int index;
  final int total;

  const _TagItem({required this.tag, required this.index, required this.total});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.tag),
      title: Text(tag.name),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            icon: const Icon(Icons.delete, size: 20, color: Colors.red),
            onPressed: () => context.read<SettingsCubit>().deleteTag(tag),
          ),
        ],
      ),
    );
  }
}
