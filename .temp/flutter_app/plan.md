# Flutter Porting Plan: Expense Tracker

## Objective
Port the existing Android Expense Tracker application to Flutter, maintaining feature parity while leveraging Flutter's cross-platform capabilities and modern ecosystem.

## Key Files & Context
- **Data Layer**: Room Entities (`ExpenseEntity`, `InstallmentEntity`, etc.) and DAOs.
- **Domain Layer**: Models (`Expense`, `Installment`) and UseCases (`AddExpenseUseCase`, `UpdateExpenseUseCase`).
- **Presentation Layer**: ViewModels and Jetpack Compose Screens.
- **Utilities**: `CsvParser`, `CurrencyFormatter`, `DateFormatterUtils`.

## Proposed Tech Stack
- **State Management**: **BLoC/Cubit** (aligns with ViewModel/UseCase architecture).
- **Local Persistence**: **Drift (Moor)** (reactive, type-safe SQLite).
- **Dependency Injection**: **GetIt** + **Injectable**.
- **Navigation**: **GoRouter** (declarative routing).
- **Charts**: **fl_chart** (for statistics).
- **OCR/Scan**: **google_ml_kit** (for receipt scanning).

## Implementation Steps

### Phase 1: Foundation & Data Layer
1. [x] **Project Setup**: Initialize Flutter project with `flutter create`.
2. [x] **Dependency Configuration**: Add `flutter_bloc`, `drift`, `get_it`, `go_router`, and other core libraries to `pubspec.yaml`.
3. [x] **Domain Models**: Port Kotlin domain models (`Expense`, `Installment`, etc.) to Dart classes.
4. [x] **Drift Database Schema**:
   - [x] Recreate tables (`expenses`, `installments`, `categories`, `tags`) using Drift's DSL.
   - [x] Implement DAOs with equivalent queries to existing `ExpenseDao`, `InstallmentDao`, etc.
   - [x] Set up database migrations and initial seed data (categories).

### Phase 2: Domain & Business Logic
1. [x] **Repository Implementation**: Port `ExpenseRepositoryImpl` and `InstallmentRepositoryImpl` to Dart, interacting with Drift DAOs.
2. [x] **UseCases/Services**: Ported all core UseCases (`AddExpense`, `UpdateExpense`, `GetExpenses`, `GetCategories`, `CreateInstallmentPlan`, etc.).
3. [x] **Utilities**:
   - [x] Ported `CurrencyFormatter` and `DateFormatterUtils`.
   - [ ] Implement `CsvParser` using a Dart CSV library.

### Phase 3: Presentation Layer (UI & State)
1. [x] **Theme & Branding**: Define `ThemeData` and implemented core UI components.
2. [x] **Navigation**: Set up `GoRouter` with declarative routes.
3. [x] **Screen-by-Screen Implementation**:
   - [x] **Expense List**: Ported `ExpenseListScreen` using Cubit with grouping, filtering, and budget tracking.
   - [x] **Add/Edit Expense**: Ported `AddExpenseScreen` with form, suggestions, and installment support.
   - [x] **Installments**: Port `InstallmentsScreen` and management logic.
   - [x] **Stats**: Recreate dashboards using `fl_chart`.
   - [x] **Settings**: Port settings and budget preferences.
4. [x] **Components**: Ported reusable UI components (chips, budget overview, etc.).

### Phase 4: Advanced Features & Refinement
1. [x] **CSV Import/Export**: Implement file handling and sharing for CSV data.
3. [ ] **Validation & Testing**:
   - [ ] Unit tests for UseCases and BLoCs.
   - [ ] Integration tests for the Drift database.
   - [ ] UI testing for key user flows.

## Verification & Testing
- **Feature Parity**: Verify all features (CRUD, Installments, Stats, Budget) work as they do in the Android app.
- **Data Integrity**: Ensure database migrations and initial seeding work correctly.
- **Performance**: Monitor list scrolling and chart rendering performance.
- **UI Consistency**: Compare Flutter UI with the original Jetpack Compose implementation for visual fidelity.
