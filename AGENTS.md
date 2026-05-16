# Repository Guidelines

## Project Structure & Module Organization

Sans Finance is a Kotlin Multiplatform (KMP) project consisting of the following modules:

- `:app` — Android application module (Jetpack Compose, Hilt, Room).
- `:shared` — Kotlin Multiplatform module containing shared domain logic, models, and interfaces.
- `:server` — Ktor-based backend server providing an API for the financial data.

### Module Details

- **app**: 
    - Source code: `app/src/main/java/com/sans/finance`.
    - UI: Jetpack Compose under `presentation/`.
    - Data: Room database and Hilt DI.
- **shared**:
    - Source code: `shared/src/commonMain/kotlin/com/sans/finance`.
    - Layers: `domain/` (models, repositories, usecases), `data/` (shared entities).
- **server**:
    - Source code: `server/src/main/kotlin/com/sans/finance/server`.
    - Framework: Ktor.
- **scripts**: Utility scripts like `backup.sh`, `sync.sh`, `push_portfolio.sh`.
- **Makefile**: Common tasks for building and running.

## Build, Test, and Development Commands

You can use the `Makefile` for convenience:

- `make run` — build and run on device
- `make build` — build debug APK
- `make release` — build and package release APK
- `make test-unit` — run JVM unit tests
- `make test-android` — run instrumentation tests

Alternatively, use the Gradle wrapper:

- `./gradlew :app:assembleDebug` — build Android debug APK
- `./gradlew :server:run` — run the Ktor server
- `./gradlew test` — run all tests

Min/target SDK is 36.

## High-Level Architecture

The project follows **Clean Architecture** with a Kotlin Multiplatform core.

### Layers (Shared & App)

**Domain** (`shared/domain/`) — Pure Kotlin.
- `model/` — Core models: `Expense`, `Account`, `PortfolioHolding`, `Goal`, `Budget`.
- `repository/` — Interfaces for data access.
- `usecase/` — Business logic (e.g., `AddTransactionUseCase`, `PredictTransactionUseCase`).

**Data** (`app/data/` & `shared/data/`)
- `local/entity/` — Room entities (database version 31).
- `local/dao/` — Room DAOs with complex queries for analytics.
- `repository/` — Implementations mapping entities to domain models.

**Presentation** (`app/presentation/`) — Compose + ViewModel.
- ViewModels use `StateFlow` to expose UI state.
- Screen list: `Dashboard`, `ExpenseList`, `AddTransaction`, `Wealth`, `Portfolio`, `Goals`, `Budgets`, `MonthlyReview`, `DebtStrategist`, `Search`, etc.
- Navigation: Type-safe routes using Kotlinx Serialization in `Screen.kt`.

## Database

Room database is at **version 31**. It includes a sophisticated tag system, category ordering, and support for portfolio tracking, goals, and budgets.
Reference snapshot: `sans_finance_db_snapshot.sqlite`.

## AI Integration

- **Cloud AI**: Support for **OpenAI** and OpenAI-compatible APIs (e.g., **OpenRouter**). Used for advanced features like "Analyze with AI" in the Monthly Review.
- **Local AI (Planned)**: Integration with LiteRT-LM for on-device insights and receipt scanning is on the roadmap but not yet implemented.

## Coding Style

- Kotlin, JDK 17, 4-space indentation.
- Follow Clean Architecture patterns—keep business logic in Use Cases.
- Use `MutableStateFlow` in ViewModels for state management.
- Small, focused `@Composable` functions.

## Testing

- Unit tests: `shared/src/commonTest` and `app/src/test`.
- Instrumentation: `app/src/androidTest`.
- Test naming: `*Test.kt`.
