import 'package:intl/intl.dart';

class DateFormatter {
  static String toMonthYear(DateTime date) {
    return DateFormat('MMMM yyyy').format(date);
  }

  static String toShortMonthYear(DateTime date) {
    return DateFormat('MMM yyyy').format(date);
  }

  static String toDateString(DateTime date) {
    return DateFormat('dd MMM yyyy').format(date);
  }

  static String toIsoDate(DateTime date) {
    return DateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(date);
  }

  static String toMonthKey(DateTime date) {
    return DateFormat('yyyy-MM').format(date);
  }

  static String formatTimestamp(int millis) {
    return DateFormat('dd MMM yyyy, hh:mm a').format(DateTime.fromMillisecondsSinceEpoch(millis));
  }
}
