import 'package:intl/intl.dart';

class DateFormatterUtils {
  static DateFormat getStandardFormatter() {
    return DateFormat('dd MMM yyyy');
  }

  static String formatDate(int millisecondsSinceEpoch) {
    return getStandardFormatter().format(DateTime.fromMillisecondsSinceEpoch(millisecondsSinceEpoch));
  }
}
