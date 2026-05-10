import 'package:csv/csv.dart';
import '../../domain/models/expense.dart';
import 'package:intl/intl.dart';

class CsvParser {
  static final _dateFormat = DateFormat('yyyy-MM-dd');

  static List<Expense> parse(String csvContent) {
    final List<List<dynamic>> rows = CsvToListConverter().convert(csvContent);
    if (rows.isEmpty) return [];

    final expenses = <Expense>[];
    
    // Skip header if it exists
    int startIndex = 0;
    if (rows[0][0].toString().toLowerCase().contains('date')) {
      startIndex = 1;
    }

    for (int i = startIndex; i < rows.length; i++) {
      final row = rows[i];
      if (row.length < 4) continue;

      final expense = _mapRowToExpense(row);
      if (expense != null) {
        expenses.add(expense);
      }
    }

    return expenses;
  }

  static Expense? _mapRowToExpense(List<dynamic> row) {
    try {
      final dateStr = row[0].toString();
      final date = _dateFormat.parse(dateStr).millisecondsSinceEpoch;

      final merchant = row[2].toString();
      final itemName = row[3].toString();
      if (itemName.isEmpty) return null;

      final qty = int.tryParse(row[4].toString()) ?? 1;
      final originalPrice = _parsePriceToCents(row[5].toString());
      final finalPrice = _parsePriceToCents(row[6].toString());
      final isInstallment = row.length > 8 && row[8].toString() == '1';

      final categoryId = _getCategoryId(itemName, merchant);

      return Expense(
        id: 0,
        date: date,
        itemName: itemName,
        amount: finalPrice,
        categoryId: categoryId,
        isRecurring: false,
        isInstallment: isInstallment,
        merchant: merchant,
        quantity: qty,
      );
    } catch (e) {
      return null;
    }
  }

  static int _parsePriceToCents(String priceStr) {
    if (priceStr.isEmpty) return 0;
    final clean = priceStr
        .replaceAll('Rp', '')
        .replaceAll(',', '')
        .replaceAll('"', '')
        .replaceAll('.', '')
        .trim();
    return (int.tryParse(clean) ?? 0) * 100;
  }

  static int _getCategoryId(String itemName, String? merchant) {
    final text = '$itemName ${merchant ?? ""}'.toLowerCase();
    
    if (_containsAny(text, ["sehat", "vitamin", "madu", "obat", "skincare", "fitness", "gym"])) {
      return 2; // Health
    }
    if (_containsAny(text, ["bensin", "grab", "gojek", "transport", "parkir", "travel"])) {
      return 4; // Transport
    }
    if (_containsAny(text, ["makan", "minum", "kopi", "coffee", "bakso", "nasi", "warung", "restoran"])) {
      return 1; // Food
    }
    if (_containsAny(text, ["belanja", "shopee", "tokopedia", "baju", "celana", "elektronik", "gadget", "hp"])) {
      return 3; // Shopping
    }
    if (_containsAny(text, ["pulsa", "kuota", "data", "wifi", "spotify", "netflix", "internet", "bill"])) {
      return 5; // Bills
    }
    
    return 6; // Others
  }

  static bool _containsAny(String text, List<String> keywords) {
    for (final keyword in keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }
}
