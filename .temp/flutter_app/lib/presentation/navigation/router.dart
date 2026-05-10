import 'package:go_router/go_router.dart';
import '../expense_list/expense_list_screen.dart';
import '../add_expense/add_expense_screen.dart';
import '../installments/installments_screen.dart';
import '../stats/stats_screen.dart';
import '../settings/settings_screen.dart';

final router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const ExpenseListScreen(),
    ),
    GoRoute(
      path: '/add-expense',
      builder: (context, state) => const AddExpenseScreen(),
    ),
    GoRoute(
      path: '/edit-expense/:id',
      builder: (context, state) {
        final id = int.parse(state.pathParameters['id']!);
        return AddExpenseScreen(expenseId: id);
      },
    ),
    GoRoute(
      path: '/installments',
      builder: (context, state) => const InstallmentsScreen(),
    ),
    GoRoute(
      path: '/stats',
      builder: (context, state) => const StatsScreen(),
    ),
    GoRoute(
      path: '/settings',
      builder: (context, state) => const SettingsScreen(),
    ),
  ],
);
