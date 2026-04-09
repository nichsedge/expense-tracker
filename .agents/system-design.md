# 1. System Goal

A **mobile-first personal expense tracker** that:

* minimizes input friction
* supports installments (core differentiator)
* works fully offline
* surfaces simple financial insights

---

# 2. High-Level Architecture

```
[ Android App ]
      |
      v
[ Local Database (Room / SQLite) ]
      |
      v
[ Domain Logic Layer ]
      |
      v
[ UI Layer (Jetpack Compose) ]
```

Optional later:

```
+ Sync Layer (Firebase / Supabase)
+ OCR / Parser Service
```

---

# 3. Architecture Pattern

Use **Clean Architecture (simplified)**:

```
Presentation (UI)
    ↓
Domain (Use Cases)
    ↓
Data (Repository)
    ↓
Local DB (Room)
```

Why:

* keeps logic clean
* easy to extend (installments, recurring, analytics)

---

# 4. Core Modules

## 4.1 Expense Module

Handles:

* CRUD expense
* filtering
* tagging

## 4.2 Installment Module

Handles:

* splitting expense into payments
* tracking remaining balance
* due dates

## 4.3 Recurring Module

Handles:

* auto-generating expenses
* monthly subscriptions

## 4.4 Analytics Module

Handles:

* aggregation
* summaries

## 4.5 Budget Module (lightweight)

Handles:

* monthly limit
* threshold alerts

---

# 5. Data Layer Design

## 5.1 Tables

### expenses

```
id (PK)
date (timestamp)
platform (enum)
merchant (string)
item_name (string)
quantity (int)
original_price (long)
final_price (long)
category_id (FK)
payment_method (enum)
status (enum)
is_impulsive (boolean)
is_recurring (boolean)
created_at
updated_at
```

---

### installments

```
id (PK)
expense_id (FK)
total_amount
monthly_payment
duration_months
remaining_balance
next_due_date
status (ongoing/paid)
created_at
```

---

### installment_payments (important)

Tracks each payment event.

```
id (PK)
installment_id (FK)
payment_date
amount
status (paid/pending)
```

---

### categories

```
id (PK)
name
icon
```

---

### recurring_rules

```
id (PK)
title
amount
category_id
interval (monthly/weekly)
next_run_date
is_active
```

---

### budgets

```
id (PK)
month (YYYY-MM)
limit_amount
current_spent
```

---

# 6. Domain Layer (Use Cases)

## Expense

* AddExpenseUseCase
* UpdateExpenseUseCase
* DeleteExpenseUseCase
* GetExpensesUseCase (filters)

---

## Installment

* CreateInstallmentPlanUseCase
* RecordInstallmentPaymentUseCase
* GetActiveInstallmentsUseCase
* GetUpcomingPaymentsUseCase

---

## Recurring

* GenerateRecurringExpensesUseCase (runs on app open)

---

## Analytics

* GetMonthlySpendingUseCase
* GetCategoryBreakdownUseCase
* GetLargestExpenseUseCase

---

## Budget

* CheckBudgetStatusUseCase

---

# 7. Data Flow Example

### Case: Add Expense with Installment

```
UI → AddExpenseUseCase
    → save expense
    → CreateInstallmentPlanUseCase
        → create installment
        → generate installment_payments
```

---

### Case: App Open

```
App Start
  → GenerateRecurringExpensesUseCase
  → UpdateInstallmentStatusUseCase
  → LoadDashboardData
```

---

# 8. UI Layer (Compose Structure)

## Screens

### 1. HomeScreen

* summary
* recent expenses
* quick add

---

### 2. AddExpenseScreen

State:

```
amount
category
notes
is_installment
installment_config
```

---

### 3. ExpenseListScreen

* filters
* search

---

### 4. InstallmentScreen

* active plans
* upcoming payments

---

### 5. AnalyticsScreen

* monthly summary
* category breakdown

---

# 9. State Management

Use:

* ViewModel (per screen)
* StateFlow / MutableStateFlow

Example:

```
AddExpenseViewModel
    - uiState: StateFlow<AddExpenseState>
```

---

# 10. Repository Layer

Interface:

```
ExpenseRepository
InstallmentRepository
RecurringRepository
AnalyticsRepository
```

Implementation:

* Local only (Room)

---

# 11. Background Processing

No need for heavy infra.

Use:

* WorkManager

Tasks:

* recurring expense generation
* installment due check

---

# 12. Performance Considerations

* Index:

    * date
    * category_id
    * installment_id

* Pagination for expense list

* Avoid heavy joins → precompute summaries if needed

---

# 13. Offline-First Strategy

* All writes → local DB
* No dependency on network

Future sync:

```
local DB → sync queue → backend
```

---

# 14. Security

Minimal but enough:

* encrypt DB (SQLCipher optional)
* no sensitive auth needed initially

---

# 15. Scalability (Future)

If you expand:

### Add Backend

```
Mobile → API → DB (Postgres)
```

### Features:

* multi-device sync
* backup
* account system

---

# 16. Biggest Risks (Realistically)

### 1. User stops logging

→ solve with:

* speed
* defaults
* repeat entry

---

### 2. Installment complexity

→ avoid:

* over-engineering
* keep fixed monthly only (no variable interest first)

---

### 3. Feature creep

→ resist:

* investments
* crypto
* stock tracking

Stay focused.

---

# 17. What Makes This Actually Valuable

Not:

* pretty charts

But:

* installment awareness
* impulse tracking
* spending visibility

---

# 18. Build Order (important)

1. Expense CRUD
2. Simple UI
3. Installment system
4. Recurring
5. Analytics
6. Budget
