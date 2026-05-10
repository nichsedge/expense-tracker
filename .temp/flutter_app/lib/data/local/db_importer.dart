import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

class DatabaseImporter {
  /// Imports an existing SQLite database file, overwriting the current one.
  /// Ensure you close the existing database connection before calling this.
  static Future<void> importDatabase(File sourceFile) async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final targetPath = p.join(dbFolder.path, 'expense_tracker.sqlite');
    
    if (await sourceFile.exists()) {
      await sourceFile.copy(targetPath);
      // Database successfully imported
    } else {
      throw Exception('Source database file not found at ${sourceFile.path}');
    }
  }
}
