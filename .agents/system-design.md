# 1. System Goal

A **comprehensive wealth management platform** that:

* minimizes input friction
* supports installments (core differentiator)
* tracks investments and net worth
* surfaces AI-powered financial insights (local & cloud)
* works fully offline with optional cloud sync/backup

---

# 2. High-Level Architecture

Sans Finance is built using **Kotlin Multiplatform (KMP)** to share business logic across platforms.

```
[ Android App (:app) ]      [ Future Web/Desktop ]
          |                        |
          +-----------+------------+
                      |
                      v
          [ Shared Module (:shared) ]
          (Domain, Use Cases, Entities)
                      |
          +-----------+------------+
          |                        |
          v                        v
[ Local DB (Room) ]        [ Remote API (:server) ]
```

---

# 3. Architecture Pattern

Use **Clean Architecture** with a shared core:

```
UI Layer (Compose / Ktor Server)
    ↓
Domain Layer (Use Cases & Models - in :shared)
    ↓
Data Layer (Repositories & Entities - in :shared / :app)
    ↓
Infrastructure (Room / Ktor Client)
```

---

# 4. Core Modules

## 4.1 Expense Module
- CRUD expense, filtering, tagging.
- FTS (Full Text Search) for transactions.

## 4.2 Installment Module
- Splitting expense into payments, tracking remaining balance, due dates.

## 4.3 Wealth & Portfolio Module
- Tracking assets (Cash, Bank, Investment, Crypto).
- Portfolio snapshots and health analysis.
- Net worth tracking over time.

## 4.4 Goals & Budgets Module
- Setting financial goals and tracking progress.
- Monthly budget limits and threshold alerts.

## 4.5 AI Insights Module
- **Cloud (Current)**: Deep analysis, monthly reviews, and complex categorization via **OpenAI** or **OpenRouter**.
- **Local (Planned)**: On-device predictions and suggestions for improved privacy and offline support.

---

# 5. Data Layer Design (Database Version 31)

The database includes tables for:
- `expenses`, `tags`, `categories`
- `installments`, `installment_payments`
- `accounts`, `account_types`, `account_aliases`
- `portfolio_holdings`, `portfolio_snapshots`, `portfolio_targets`
- `goals`, `budgets`
- `net_worth_snapshots`
- `exchange_rates` (for currency conversion)

---

# 6. Domain Layer (Use Cases)

- **Transaction**: `AddTransactionUseCase`, `GetExpensesUseCase`, `DeleteExpenseUseCase`.
- **Installment**: `CreateInstallmentPlanUseCase`, `RecordInstallmentPaymentUseCase`.
- **Wealth**: `GetPortfolioHealthUseCase`, `PredictTransactionUseCase`.
- **Currency**: `ConvertCurrencyUseCase`.
- **AI**: `GetDetailsSuggestionsUseCase`, `GetMonthlyReviewUseCase`.

---

# 7. UI Layer (Compose Structure)

## Primary Screens
1. **Dashboard**: Summary of net worth, recent activity, and quick actions.
2. **Wealth/Portfolio**: Asset distribution and investment tracking.
3. **Monthly Review**: AI-driven analysis of the past month.
4. **Debt Strategist**: Optimization plans for debt repayment.
5. **Budgets/Goals**: Financial planning and progress tracking.
6. **Search**: Advanced filtering and search for all transactions.

---

# 8. Offline-First Strategy

- All writes are directed to the local database first.
- The `:server` module provides an API for future multi-device synchronization and backup.
- AI operations fallback to local models if the network is unavailable or if privacy is prioritized.
