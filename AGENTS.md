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

## Remote Android Debugging & Deployment (Tailscale + Wireless ADB)

To deploy and debug on physical Android devices remotely without a USB cable:

1. **Verify Tailscale Connection**:
   - Check device IP via `tailscale status` (e.g. `100.110.101.84 xiaomi-14t-pro`).
2. **Wireless Debugging & Pairing**:
   - On Android (Developer Options): Enable **Wireless debugging** (and **Install via USB** on Xiaomi/HyperOS).
   - Tap *Pair device with pairing code*.
   - Run `adb pair <TAILSCALE_IP>:<PAIRING_PORT> <6_DIGIT_CODE>`.
3. **ADB Connect & Port Discovery**:
   - Android uses a dynamic port for the ADB daemon (separate from the pairing port). Connect via `adb connect <TAILSCALE_IP>:<PORT>`.
4. **Build & Install**:
   - Build: `./gradlew :app:assembleDebug`
   - Install: `adb -s <TAILSCALE_IP>:<PORT> install -r app/build/outputs/apk/debug/app-debug.apk`

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

Room database is at **version 33**. It includes:
- Multi-currency valuation with historical FX rates (`fx_rates` table via `FxRateEntity`)
- Configurable tag visibility and ordering (`tags` table via `TagEntity`)
- Custom account ordering and liability classifications (`account_types` and `accounts`)
- Account alias mapping (`account_aliases`)
- Support for portfolio tracking, targets (`portfolio_targets`), goals, and budgets.
Reference snapshot: `sans_finance_db_snapshot.sqlite`.

## Cloud Sync & Backup

- **Cloudflare R2 (Default)**: Pure Kotlin AWS SigV4 signed requests for S3-compatible cloud snapshot downloads and SQLite database backups.
- **Google Cloud Storage (GCS, Optional)**: Service-account JWT authentication for snapshots and SQLite database backup uploads.
- Background sync and automated backups scheduled via Android `WorkManager` with exponential backoff retry policies.

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
