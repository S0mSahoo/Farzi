import 'transaction_item.dart';

class BudgetModel {
  final int id;
  final String monthKey; // Format: "YYYY-MM"
  final double totalBudget;
  final Map<TransactionCategory, double> categoryBudgets;
  final int createdAt;
  final int updatedAt;

  BudgetModel({
    this.id = 0,
    required this.monthKey,
    required this.totalBudget,
    this.categoryBudgets = const {},
    int? createdAt,
    int? updatedAt,
  })  : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch,
        updatedAt = updatedAt ?? DateTime.now().millisecondsSinceEpoch;

  BudgetModel copyWith({
    int? id,
    String? monthKey,
    double? totalBudget,
    Map<TransactionCategory, double>? categoryBudgets,
    int? createdAt,
    int? updatedAt,
  }) {
    return BudgetModel(
      id: id ?? this.id,
      monthKey: monthKey ?? this.monthKey,
      totalBudget: totalBudget ?? this.totalBudget,
      categoryBudgets: categoryBudgets ?? this.categoryBudgets,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toJson() {
    final catMap = <String, double>{};
    categoryBudgets.forEach((k, v) {
      catMap[k.name] = v;
    });

    return {
      'id': id,
      'monthKey': monthKey,
      'totalBudget': totalBudget,
      'categoryBudgets': catMap,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }

  factory BudgetModel.fromJson(Map<String, dynamic> json) {
    final catMapRaw = json['categoryBudgets'] as Map<String, dynamic>? ?? {};
    final parsedCatBudgets = <TransactionCategory, double>{};
    catMapRaw.forEach((k, v) {
      try {
        final cat = TransactionCategory.values.firstWhere((e) => e.name == k);
        parsedCatBudgets[cat] = (v as num).toDouble();
      } catch (_) {}
    });

    return BudgetModel(
      id: json['id'] is int ? json['id'] : 0,
      monthKey: json['monthKey'] as String? ?? '',
      totalBudget: (json['totalBudget'] as num?)?.toDouble() ?? 0.0,
      categoryBudgets: parsedCatBudgets,
      createdAt: json['createdAt'] as int?,
      updatedAt: json['updatedAt'] as int?,
    );
  }
}
