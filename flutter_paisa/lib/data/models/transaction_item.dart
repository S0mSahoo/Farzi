enum TransactionType {
  EXPENSE,
  INCOME,
}

enum TransactionCategory {
  FOOD,
  TRANSPORT,
  SHOPPING,
  BILLS,
  ENTERTAINMENT,
  HEALTH,
  EDUCATION,
  INVESTMENT,
  SALARY,
  BUSINESS,
  FREELANCE,
  GIFT,
  OTHER,
}

enum PaymentMethod {
  UPI,
  CASH,
  CARD,
  NET_BANKING,
  OTHER,
}

class TransactionItem {
  final int id;
  final String title;
  final double amount;
  final TransactionType type;
  final TransactionCategory category;
  final PaymentMethod paymentMethod;
  final int timestamp;
  final String note;
  final int? recurringRuleId;
  final int createdAt;
  final int updatedAt;

  TransactionItem({
    this.id = 0,
    required this.title,
    required this.amount,
    required this.type,
    required this.category,
    this.paymentMethod = PaymentMethod.UPI,
    required this.timestamp,
    this.note = '',
    this.recurringRuleId,
    int? createdAt,
    int? updatedAt,
  })  : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch,
        updatedAt = updatedAt ?? DateTime.now().millisecondsSinceEpoch;

  TransactionItem copyWith({
    int? id,
    String? title,
    double? amount,
    TransactionType? type,
    TransactionCategory? category,
    PaymentMethod? paymentMethod,
    int? timestamp,
    String? note,
    int? recurringRuleId,
    int? createdAt,
    int? updatedAt,
  }) {
    return TransactionItem(
      id: id ?? this.id,
      title: title ?? this.title,
      amount: amount ?? this.amount,
      type: type ?? this.type,
      category: category ?? this.category,
      paymentMethod: paymentMethod ?? this.paymentMethod,
      timestamp: timestamp ?? this.timestamp,
      note: note ?? this.note,
      recurringRuleId: recurringRuleId ?? this.recurringRuleId,
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
      'timestamp': timestamp,
      'note': note,
      'recurringRuleId': recurringRuleId,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }

  factory TransactionItem.fromJson(Map<String, dynamic> json) {
    return TransactionItem(
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
      timestamp: json['timestamp'] is int ? json['timestamp'] : DateTime.now().millisecondsSinceEpoch,
      note: json['note'] as String? ?? '',
      recurringRuleId: json['recurringRuleId'] as int?,
      createdAt: json['createdAt'] as int?,
      updatedAt: json['updatedAt'] as int?,
    );
  }
}
