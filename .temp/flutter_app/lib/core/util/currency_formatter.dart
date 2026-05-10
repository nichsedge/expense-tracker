import 'package:intl/intl.dart';

class CurrencyFormatter {
  static String formatAmount(int amountInCents) {
    final formatter = NumberFormat.currency(locale: 'en_US', symbol: '\$');
    return formatter.format(amountInCents / 100.0);
  }
}
