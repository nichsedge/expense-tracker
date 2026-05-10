import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../core/util/currency_formatter.dart';
import '../../di/injection.dart';
import 'cubit/stats_cubit.dart';
import 'cubit/stats_state.dart';

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => getIt<StatsCubit>(),
      child: const StatsView(),
    );
  }
}

class StatsView extends StatelessWidget {
  const StatsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Statistics', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocBuilder<StatsCubit, StatsState>(
        builder: (context, state) {
          if (state.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state.error != null) {
            return Center(child: Text('Error: ${state.error}'));
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _HeaderPart(
                thisMonth: state.thisMonthSpent,
                lastMonth: state.lastMonthSpent,
              ),
              const SizedBox(height: 24),
              _TrendSection(
                trendData: state.trendSpending,
                selectedPeriod: state.selectedTrendPeriod,
              ),
              const SizedBox(height: 24),
              _CategoryBreakdown(categories: state.spendingByCategory),
              const SizedBox(height: 24),
              _YearlySummary(
                thisYear: state.thisYearSpent,
                lastYear: state.lastYearSpent,
              ),
            ],
          );
        },
      ),
    );
  }
}

class _HeaderPart extends StatelessWidget {
  final int thisMonth;
  final int lastMonth;

  const _HeaderPart({required this.thisMonth, required this.lastMonth});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final diff = thisMonth - lastMonth;
    final percent = lastMonth > 0 ? (diff / lastMonth * 100).toInt() : 0;

    return Column(
      children: [
        Text(
          'THIS MONTH',
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.secondary,
            letterSpacing: 1.5,
          ),
        ),
        Text(
          CurrencyFormatter.formatAmount(thisMonth),
          style: theme.textTheme.displaySmall?.copyWith(
            fontWeight: FontWeight.w900,
            color: theme.colorScheme.primary,
          ),
        ),
        if (lastMonth > 0)
          Chip(
            backgroundColor: diff > 0 ? theme.colorScheme.errorContainer : theme.colorScheme.secondaryContainer,
            label: Text(
              '${diff > 0 ? "+" : ""}$percent% from last month',
              style: theme.textTheme.labelSmall?.copyWith(
                color: diff > 0 ? theme.colorScheme.error : theme.colorScheme.secondary,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
      ],
    );
  }
}

class _TrendSection extends StatelessWidget {
  final Map<int, int> trendData;
  final TrendPeriod selectedPeriod;

  const _TrendSection({required this.trendData, required this.selectedPeriod});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionTitle(title: 'Spending Trend', icon: Icons.insights),
        const SizedBox(height: 12),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: TrendPeriod.values.map((period) {
              final isSelected = selectedPeriod == period;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(period.name.toUpperCase()),
                  selected: isSelected,
                  onSelected: (_) => context.read<StatsCubit>().onTrendPeriodSelected(period),
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 16),
        AspectRatio(
          aspectRatio: 1.7,
          child: Card(
            elevation: 0,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(0.3),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: _LineChart(data: trendData),
            ),
          ),
        ),
      ],
    );
  }
}

class _LineChart extends StatelessWidget {
  final Map<int, int> data;

  const _LineChart({required this.data});

  @override
  Widget build(BuildContext context) {
    if (data.isEmpty) return const Center(child: Text('No data'));

    final sortedKeys = data.keys.toList()..sort();
    final spots = sortedKeys.asMap().entries.map((e) {
      return FlSpot(e.key.toDouble(), data[e.value]!.toDouble());
    }).toList();

    return LineChart(
      LineChartData(
        gridData: const FlGridData(show: false),
        titlesData: const FlTitlesData(show: false),
        borderData: FlBorderData(show: false),
        lineBarsData: [
          LineChartBarData(
            spots: spots,
            isCurved: true,
            color: Theme.of(context).colorScheme.primary,
            barWidth: 4,
            isStrokeCapRound: true,
            dotData: const FlDotData(show: false),
            belowBarData: BarAreaData(
              show: true,
              color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryBreakdown extends StatelessWidget {
  final List<dynamic> categories; // Use CategorySpent

  const _CategoryBreakdown({required this.categories});

  @override
  Widget build(BuildContext context) {
    final total = categories.fold<int>(0, (sum, item) => sum + (item.totalAmount as int));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionTitle(title: 'By Category', icon: Icons.pie_chart),
        Card(
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Column(
              children: categories.map((cat) {
                final percent = total > 0 ? cat.totalAmount / total : 0.0;
                return ListTile(
                  leading: CircleAvatar(
                    backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                    child: Text(cat.categoryIcon, style: const TextStyle(fontSize: 20)),
                  ),
                  title: Text(cat.categoryName, style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: LinearProgressIndicator(
                    value: percent,
                    backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  trailing: Text(
                    CurrencyFormatter.formatAmount(cat.totalAmount),
                    style: const TextStyle(fontWeight: FontWeight.w900),
                  ),
                );
              }).toList(),
            ),
          ),
        ),
      ],
    );
  }
}

class _YearlySummary extends StatelessWidget {
  final int thisYear;
  final int lastYear;

  const _YearlySummary({required this.thisYear, required this.lastYear});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionTitle(title: 'Yearly Summary', icon: Icons.calendar_month),
        Row(
          children: [
            Expanded(
              child: _StatsSmallCard(
                title: 'This Year',
                amount: thisYear,
                color: Theme.of(context).colorScheme.primaryContainer,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _StatsSmallCard(
                title: 'Last Year',
                amount: lastYear,
                color: Theme.of(context).colorScheme.secondaryContainer,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _StatsSmallCard extends StatelessWidget {
  final String title;
  final int amount;
  final Color color;

  const _StatsSmallCard({required this.title, required this.amount, required this.color});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: color,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.labelMedium),
            Text(
              CurrencyFormatter.formatAmount(amount),
              style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionTitle({required this.title, required this.icon});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Icon(icon, size: 20, color: theme.colorScheme.secondary),
          const SizedBox(width: 12),
          Text(
            title.toUpperCase(),
            style: theme.textTheme.labelLarge?.copyWith(
              color: theme.colorScheme.secondary,
              letterSpacing: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
