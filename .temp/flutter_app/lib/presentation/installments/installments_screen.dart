import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/util/currency_formatter.dart';
import '../../core/util/date_formatter_utils.dart';
import '../../di/injection.dart';
import '../../domain/models/installment.dart';
import '../../domain/models/installment_item.dart';
import 'cubit/installment_list_cubit.dart';
import 'cubit/installment_list_state.dart';

class InstallmentsScreen extends StatelessWidget {
  const InstallmentsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => getIt<InstallmentListCubit>(),
      child: const InstallmentsView(),
    );
  }
}

class InstallmentsView extends StatelessWidget {
  const InstallmentsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Active Installments', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocBuilder<InstallmentListCubit, InstallmentListState>(
        builder: (context, state) {
          return Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _SummaryCard(
                  monthlyDue: state.totalMonthlyDue,
                  remainingBalance: state.totalRemainingBalance,
                ),
                const SizedBox(height: 16),
                DefaultTabController(
                  length: 2,
                  initialIndex: state.selectedTab,
                  child: Column(
                    children: [
                      TabBar(
                        onTap: (index) => context.read<InstallmentListCubit>().onTabSelected(index),
                        tabs: const [
                          Tab(text: 'Active'),
                          Tab(text: 'History'),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Expanded(
                  child: _InstallmentList(
                    installments: state.selectedTab == 0 ? state.activeInstallments : state.historyInstallments,
                    isHistory: state.selectedTab == 1,
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final int monthlyDue;
  final int remainingBalance;

  const _SummaryCard({required this.monthlyDue, required this.remainingBalance});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      color: theme.colorScheme.secondaryContainer,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'TOTAL MONTHLY DUE',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSecondaryContainer.withAlpha(178),
              ),
            ),
            Text(
              CurrencyFormatter.formatAmount(monthlyDue),
              style: theme.textTheme.headlineLarge?.copyWith(
                fontWeight: FontWeight.w900,
                color: theme.colorScheme.onSecondaryContainer,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Total Remaining Balance: ${CurrencyFormatter.formatAmount(remainingBalance)}',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSecondaryContainer.withAlpha(204),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InstallmentList extends StatelessWidget {
  final List<Installment> installments;
  final bool isHistory;

  const _InstallmentList({required this.installments, required this.isHistory});

  @override
  Widget build(BuildContext context) {
    if (installments.isEmpty) {
      return Center(
        child: Text(
          isHistory ? 'No history' : 'No active installments',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
      );
    }

    return ListView.separated(
      itemCount: installments.length,
      separatorBuilder: (context, index) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        return _ExpandableInstallmentItem(installment: installments[index]);
      },
    );
  }
}

class _ExpandableInstallmentItem extends StatefulWidget {
  final Installment installment;

  const _ExpandableInstallmentItem({required this.installment});

  @override
  State<_ExpandableInstallmentItem> createState() => _ExpandableInstallmentItemState();
}

class _ExpandableInstallmentItemState extends State<_ExpandableInstallmentItem> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final installment = widget.installment;

    return Card(
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surfaceContainerHighest,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => setState(() => _isExpanded = !_isExpanded),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          installment.expenseName ?? 'Installment Plan #${installment.id}',
                          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        if (installment.expenseDate != null)
                          Text(
                            DateFormatterUtils.formatDate(installment.expenseDate!),
                            style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                          ),
                      ],
                    ),
                  ),
                  Row(
                    children: [
                      Text(
                        '${CurrencyFormatter.formatAmount(installment.monthlyPayment)}/mo',
                        style: theme.textTheme.titleMedium?.copyWith(
                          color: theme.colorScheme.primary,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      Icon(_isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down),
                    ],
                  ),
                ],
              ),
              if (_isExpanded) ...[
                const SizedBox(height: 16),
                const Divider(height: 1),
                const SizedBox(height: 12),
                StreamBuilder<List<InstallmentItem>>(
                  stream: context.read<InstallmentListCubit>().getItemsForInstallment(installment.id),
                  builder: (context, snapshot) {
                    if (!snapshot.hasData) return const CircularProgressIndicator();
                    final items = snapshot.data!;
                    return Column(
                      children: items.map((item) => _MonthlyPaymentRow(item: item)).toList(),
                    );
                  },
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _MonthlyPaymentRow extends StatelessWidget {
  final InstallmentItem item;

  const _MonthlyPaymentRow({required this.item});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isPaid = item.status == 'Paid';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Month ${item.monthNumber}',
                style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              Text(
                DateFormatterUtils.formatDate(item.dueDate),
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
          Row(
            children: [
              Text(
                isPaid ? 'PAID' : 'PENDING',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: isPaid ? theme.colorScheme.primary : theme.colorScheme.error,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                onPressed: () => context.read<InstallmentListCubit>().toggleStatus(item.id, item.status),
                icon: Icon(
                  isPaid ? Icons.check_circle : Icons.radio_button_unchecked,
                  color: isPaid ? theme.colorScheme.primary : theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
