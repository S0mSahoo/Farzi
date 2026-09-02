import 'dart:io';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'package:share_plus/share_plus.dart';
import '../models/transaction_item.dart';
import '../models/user_profile.dart';

enum ExportPeriod {
  CURRENT_MONTH,
  SELECTED_YEAR,
  CUSTOM_RANGE,
  ALL_TIME,
}

class PdfExportService {
  static Future<void> generateAndSharePdf({
    required UserProfile profile,
    required List<TransactionItem> transactions,
    required String periodTitle,
  }) async {
    final pdf = pw.Document();

    final totalIncome = transactions
        .where((t) => t.type == TransactionType.INCOME)
        .fold(0.0, (sum, t) => sum + t.amount);

    final totalExpense = transactions
        .where((t) => t.type == TransactionType.EXPENSE)
        .fold(0.0, (sum, t) => sum + t.amount);

    final netSavings = totalIncome - totalExpense;

    final dateFormat = DateFormat('dd MMM yyyy');
    final currency = profile.currencySymbol;

    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        header: (pw.Context context) {
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Text(
                    'PAISA - Financial Statement',
                    style: pw.TextStyle(fontSize: 18, fontWeight: pw.FontWeight.bold, color: PdfColors.indigo800),
                  ),
                  pw.Text(
                    DateFormat('dd MMM yyyy, HH:mm').format(DateTime.now()),
                    style: const pw.TextStyle(fontSize: 10, color: PdfColors.grey700),
                  ),
                ],
              ),
              pw.Text(
                'Account: ${profile.name.isNotEmpty ? profile.name : profile.email} (${profile.email})',
                style: const pw.TextStyle(fontSize: 10, color: PdfColors.grey600),
              ),
              pw.Text(
                'Period: $periodTitle',
                style: pw.TextStyle(fontSize: 11, fontWeight: pw.FontWeight.bold, color: PdfColors.grey900),
              ),
              pw.Divider(thickness: 1, color: PdfColors.indigo200),
              pw.SizedBox(height: 8),
            ],
          );
        },
        build: (pw.Context context) {
          return [
            // Summary Cards
            pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              children: [
                _buildSummaryBox('Total Income', '$currency ${totalIncome.toStringAsFixed(2)}', PdfColors.green700),
                _buildSummaryBox('Total Expense', '$currency ${totalExpense.toStringAsFixed(2)}', PdfColors.red700),
                _buildSummaryBox('Net Savings', '$currency ${netSavings.toStringAsFixed(2)}', netSavings >= 0 ? PdfColors.indigo700 : PdfColors.red700),
              ],
            ),
            pw.SizedBox(height: 16),
            pw.Text(
              'Itemized Transactions (${transactions.length} records)',
              style: pw.TextStyle(fontSize: 13, fontWeight: pw.FontWeight.bold),
            ),
            pw.SizedBox(height: 8),
            // Transactions Table
            pw.TableHelper.fromTextArray(
              headers: ['Date', 'Title', 'Category', 'Mode', 'Type', 'Amount'],
              headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, color: PdfColors.white, fontSize: 9),
              headerDecoration: const pw.BoxDecoration(color: PdfColors.indigo700),
              cellHeight: 22,
              cellStyle: const pw.TextStyle(fontSize: 8),
              data: transactions.map((t) {
                final isInc = t.type == TransactionType.INCOME;
                return [
                  dateFormat.format(DateTime.fromMillisecondsSinceEpoch(t.timestamp)),
                  t.title,
                  t.category.name,
                  t.paymentMethod.name,
                  isInc ? 'Income' : 'Expense',
                  '${isInc ? '+' : '-'} $currency ${t.amount.toStringAsFixed(2)}',
                ];
              }).toList(),
            ),
          ];
        },
      ),
    );

    final output = await getTemporaryDirectory();
    final file = File('${output.path}/Paisa_Statement_${DateTime.now().millisecondsSinceEpoch}.pdf');
    await file.writeAsBytes(await pdf.save());

    await Share.shareXFiles(
      [XFile(file.path)],
      subject: 'Paisa Financial Statement - $periodTitle',
      text: 'Here is your generated financial statement from Paisa.',
    );
  }

  static pw.Widget _buildSummaryBox(String label, String value, PdfColor color) {
    return pw.Container(
      padding: const pw.EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: pw.BoxDecoration(
        border: pw.Border.all(color: color, width: 1),
        borderRadius: const pw.BorderRadius.all(pw.Radius.circular(6)),
      ),
      child: pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.start,
        children: [
          pw.Text(label, style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700)),
          pw.SizedBox(height: 4),
          pw.Text(value, style: pw.TextStyle(fontSize: 12, fontWeight: pw.FontWeight.bold, color: color)),
        ],
      ),
    );
  }
}
