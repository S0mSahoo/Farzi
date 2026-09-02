class UserProfile {
  final String name;
  final String email;
  final String photoUrl;
  final String currencySymbol;
  final String currencyCode;
  final bool hasCompletedOnboarding;

  const UserProfile({
    this.name = '',
    this.email = '',
    this.photoUrl = '',
    this.currencySymbol = '₹',
    this.currencyCode = 'INR',
    this.hasCompletedOnboarding = false,
  });

  UserProfile copyWith({
    String? name,
    String? email,
    String? photoUrl,
    String? currencySymbol,
    String? currencyCode,
    bool? hasCompletedOnboarding,
  }) {
    return UserProfile(
      name: name ?? this.name,
      email: email ?? this.email,
      photoUrl: photoUrl ?? this.photoUrl,
      currencySymbol: currencySymbol ?? this.currencySymbol,
      currencyCode: currencyCode ?? this.currencyCode,
      hasCompletedOnboarding: hasCompletedOnboarding ?? this.hasCompletedOnboarding,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'email': email,
      'photoUrl': photoUrl,
      'currencySymbol': currencySymbol,
      'currencyCode': currencyCode,
      'hasCompletedOnboarding': hasCompletedOnboarding,
    };
  }

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      name: json['name'] as String? ?? '',
      email: json['email'] as String? ?? '',
      photoUrl: json['photoUrl'] as String? ?? '',
      currencySymbol: json['currencySymbol'] as String? ?? '₹',
      currencyCode: json['currencyCode'] as String? ?? 'INR',
      hasCompletedOnboarding: json['hasCompletedOnboarding'] as bool? ?? false,
    );
  }
}
