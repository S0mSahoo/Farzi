# Paisa (Flutter & Dart)

A modern, offline-first personal finance and expense management application built with Flutter, Dart, Riverpod, and Google Drive cloud synchronization.

## Features Preserved from Paisa Android
- **Google Sign-In & Authentication**: Retrieve display name, email, and avatar. Strict per-account isolation.
- **Google Drive Cloud Sync**: Persistent cloud source of truth stored in `appDataFolder` as `paisa_finance_backup.json`. Compatible with existing Android data.
- **Automatic Sync on Resume**: Automatic synchronization when opening or resuming the application.
- **Data Portability**:
  - **PDF Financial Statements**: Categorized monthly, yearly, or all-time reports.
  - **JSON Schema v1 Export & Merge Import**: Idempotent data merging without duplicate transactions.
- **Financial Features**:
  - Income & Expense tracking with categories and payment methods.
  - Interactive Month Picker and Personalized First-Name greeting (`Namaste, <First Name> 👋`).
  - Monthly budget limits and progress indicators.
  - Recurring transaction rules and automations.
  - Interactive calendar with daily breakdown.
- **Visual Design & Themes**:
  - Automatic System Light vs AMOLED Dark Mode (`#000000` canvas).
  - Proper Material Design 3 styling and responsive safe area padding.

## Project Structure
```
flutter_paisa/
├── pubspec.yaml
├── lib/
│   ├── main.dart
│   ├── core/
│   │   ├── theme/
│   │   │   └── app_theme.dart
│   │   └── utils/
│   │       ├── currency_formatter.dart
│   │       └── date_formatter.dart
│   ├── data/
│   │   ├── models/
│   │   │   ├── transaction_item.dart
│   │   │   ├── budget_model.dart
│   │   │   ├── recurring_rule.dart
│   │   │   ├── user_profile.dart
│   │   │   └── backup_payload.dart
│   │   ├── services/
│   │   │   ├── google_auth_service.dart
│   │   │   ├── google_drive_service.dart
│   │   │   ├── json_portability_service.dart
│   │   │   └── pdf_export_service.dart
│   │   └── repositories/
│   │       └── finance_repository.dart
│   ├── state/
│   │   ├── finance_state.dart
│   │   └── finance_notifier.dart
│   └── features/
│       ├── auth/
│       ├── dashboard/
│       ├── transactions/
│       ├── calendar/
│       ├── budget/
│       ├── recurring/
│       ├── settings/
│       ├── export/
│       └── main_navigation/
```

## How to Run

1. Navigate to the project directory:
   ```bash
   cd flutter_paisa
   ```

2. Fetch Flutter dependencies:
   ```bash
   flutter pub get
   ```

3. Run on your connected device or emulator:
   ```bash
   flutter run
   ```
