import 'transaction_item.dart';

enum RecurrenceInterval {
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY,
}

class RecurringRule {
  final int id;
  final String title;
  final double amount;
  final TransactionType type;
  final TransactionCategory category;
  final PaymentMethod paymentMethod;
  final RecurrenceInterval interval;
  final int startDate;
  final int? endDate;
  final int lastProcessedDate;
  final bool isActive;
  final String note;
  final int createdAt;
  final int updatedAt;

  RecurringRule({
    this.id = 0,
    required this.title,
    required this.amount,
    required this.type,
    required this.category,
    this.paymentMethod = PaymentMethod.UPI,
    required this.interval,
    required this.startDate,
    this.endDate,
    required this.lastProcessedDate,
    this.isActive = true,
    this.note = '',
    int? createdAt,
    int? updatedAt,
  })  : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch,
        updatedAt = updatedAt ?? DateTime.now().millisecondsSinceEpoch;

  RecurringRule copyWith({
    int? id,
    String? title,
    double? amount,
    TransactionType? type,
    TransactionCategory? category,
    PaymentMethod? paymentMethod,
    RecurrenceInterval? interval,
    int? startDate,
    int? endDate,
    int? lastProcessedDate,
    bool? isActive,
    String? note,
    int? createdAt,
    int? updatedAt,
  }) {
    return RecurringRule(
      id: id ?? this.id,
      title: title ?? this.title,
      amount: amount ?? this.amount,
      type: type ?? this.type,
      category: category ?? this.category,
      paymentMethod: paymentMethod ?? this.paymentMethod,
      interval: interval ?? this.interval,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      lastProcessedDate: lastProcessedDate ?? this.lastProcessedDate,
      isActive: isActive ?? this.isActive,
      note: note ?? this.note,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'amount': amount,
      'type': type.name,
      'category': category.name,
      'paymentMethod': paymentMethod.name,
      'interval': interval.name,
      'startDate': startDate,
      'endDate': endDate,
      'lastProcessedDate': lastProcessedDate,
      'isActive': isActive,
      'note': note,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }

  factory RecurringRule.fromJson(Map<String, dynamic> json) {
    return RecurringRule(
      id: json['id'] is int ? json['id'] : 0,
      title: json['title'] as String? ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      type: TransactionType.values.firstWhere(
        (e) => e.name == json['type'],
        orElse: () => TransactionType.EXPENSE,
      ),
      category: TransactionCategory.values.firstWhere(
        (e) => e.name == json['category'],
        orElse: () => TransactionCategory.OTHER,
      ),
      paymentMethod: PaymentMethod.values.firstWhere(
        (e) => e.name == json['paymentMethod'],
        orElse: () => PaymentMethod.UPI,
      ),
      interval: RecurrenceInterval.values.firstWhere(
        (e) => e.name == json['interval'],
        orElse: () => RecurrenceInterval.MONTHLY,
      ),
      startDate: json['startDate'] is int ? json['startDate'] : DateTime.now().millisecondsSinceEpoch,
      endDate: json['endDate'] as int?,
      lastProcessedDate: json['lastProcessedDate'] is int ? json['lastProcessedDate'] : 0,
      isActive: json['isActive'] as bool? ?? true,
      note: json['note'] as String? ?? '',
      createdAt: json['createdAt'] as int?,
      updatedAt: json['updatedAt'] as int?,
    );
  }
}
