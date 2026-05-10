import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import '../../domain/repository/expense_repository.dart';
import '../../domain/models/expense.dart';
import '../../di/injection.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _repository = getIt<ExpenseRepository>();
  List<Expense> _expenses = [];

  @override
  void initState() {
    super.initState();
    _loadExpenses();
  }

  void _loadExpenses() {
    _repository.getAllExpenses().listen((expenses) {
      setState(() {
        _expenses = expenses;
      });
    });
  }

  Future<void> _importDatabase() async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.any, // SQLite files often don't have a standard extension on Android
      );

      if (result != null && result.files.single.path != null) {
        final sourceFile = File(result.files.single.path!);
        
        // Read bytes from the picked file
        final bytes = await sourceFile.readAsBytes();
        
        final dbFolder = await getApplicationDocumentsDirectory();
        final targetPath = p.join(dbFolder.path, 'expense_tracker.sqlite');
        final targetFile = File(targetPath);
        
        await targetFile.writeAsBytes(bytes);
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Database imported! Please restart the app to see changes.')),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error importing: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Expense Tracker (Flutter Port)'),
        actions: [
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: 'Import SQLite DB',
            onPressed: _importDatabase,
          ),
        ],
      ),
      body: _expenses.isEmpty
          ? const Center(child: Text('No expenses found. Try importing the database!'))
          : ListView.builder(
              itemCount: _expenses.length,
              itemBuilder: (context, index) {
                final expense = _expenses[index];
                final date = DateTime.fromMillisecondsSinceEpoch(expense.date);
                return ListTile(
                  title: Text(expense.itemName),
                  subtitle: Text('${date.day}/${date.month}/${date.year} - ${expense.merchant ?? 'Unknown'}'),
                  trailing: Text(
                    '\$${expense.amount / 100}', 
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                );
              },
            ),
    );
  }
}
