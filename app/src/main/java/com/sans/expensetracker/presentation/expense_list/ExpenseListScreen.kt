package com.sans.expensetracker.presentation.expense_list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.sans.expensetracker.R
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.presentation.components.CategoryIcon
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpenseClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }


    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onInstallmentsClick) {
                        Icon(Icons.Default.Payments, contentDescription = "Active Installments")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_expenses)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            val isFiltered = state.selectedCategoryId != null || 
                                           state.minAmount != null || 
                                           state.maxAmount != null || 
                                           state.selectedTags.isNotEmpty()
                            Icon(
                                Icons.Default.Tune, 
                                contentDescription = "Filters",
                                tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            SummaryCard(
                thisMonth = state.thisMonthSpent,
                periodTotal = state.totalFilteredAmount
            )
            
            Text(
                stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold
            )

            DateRangeFilterBar(
                currentStart = state.startDate,
                onRangeSelected = { start, end ->
                    viewModel.updateDateRange(start, end)
                }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.groupedExpenses.forEach { (date, expenses) ->
                    item(key = "header-$date") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                date,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    items(
                        items = expenses,
                        key = { it.id },
                        contentType = { "expense" }
                    ) { expense ->
                        ExpenseItem(
                            expense = expense,
                            category = state.categories.find { it.id == expense.categoryId },
                            onClick = { onExpenseClick(expense.id) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expenseToDelete = expense
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                expenseToDelete = null
            },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        showDeleteDialog = false
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    expenseToDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showFilterSheet) {
        AdvancedFilterSheet(
            state = state,
            onDismiss = { showFilterSheet = false },
            onCategorySelected = { viewModel.updateCategoryFilter(it) },
            onAmountFilterChanged = { min, max -> viewModel.updateAmountFilter(min, max) },
            onTagToggle = { viewModel.toggleTagFilter(it) },
            onClearFilters = { 
                viewModel.clearFilters()
                showFilterSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    state: ExpenseListState,
    onDismiss: () -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onAmountFilterChanged: (Long?, Long?) -> Unit,
    onTagToggle: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var minAmountStr by remember { mutableStateOf(state.minAmount?.let { kotlin.math.ceil(it / 100.0).toLong().toString() } ?: "") }
    var maxAmountStr by remember { mutableStateOf(state.maxAmount?.let { kotlin.math.ceil(it / 100.0).toLong().toString() } ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.clear_filters))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            CategoryIcon(icon = category.icon, fontSize = 14.sp)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.amount_spent),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = minAmountStr,
                    onValueChange = { 
                        minAmountStr = it
                        onAmountFilterChanged(it.toLongOrNull()?.let { v -> v * 100 }, maxAmountStr.toLongOrNull()?.let { v -> v * 100 })
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.min_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxAmountStr,
                    onValueChange = { 
                        maxAmountStr = it
                        onAmountFilterChanged(minAmountStr.toLongOrNull()?.let { v -> v * 100 }, it.toLongOrNull()?.let { v -> v * 100 })
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.max_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (state.availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.tags),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = state.selectedTags.contains(tag),
                            onClick = { onTagToggle(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.apply_filters))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryCard(thisMonth: Long, periodTotal: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.this_month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(thisMonth),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.total_filtered),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(periodTotal),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense, 
    category: com.sans.expensetracker.data.local.entity.CategoryEntity?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val icon = category?.icon ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CategoryIcon(
                        icon = icon,
                        fontSize = 20.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val merchantDisplay = when {
                    !expense.merchant.isNullOrBlank() -> "${expense.merchant} • "
                    expense.tags.isNotEmpty() -> "${expense.tags.joinToString(", ")} • "
                    else -> ""
                }
                Text(
                    "$merchantDisplay${category?.name ?: stringResource(R.string.uncategorized)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.isInstallment && expense.monthlyPayment > 0) {
                    val totalPaid = com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(expense.totalPaid)
                    val totalAmount = com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(expense.amount)
                    Text(
                        "Paid: $totalPaid / $totalAmount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val displayAmount = if (expense.isInstallment && expense.monthlyPayment > 0) expense.monthlyPayment else expense.amount
                Text(
                    com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(displayAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterBar(
    currentStart: Long,
    onRangeSelected: (Long, Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    
    // Normalize today for comparisons
    val today = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    // 7 Days
    val sevenDaysAgo = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    
    // 30 Days
    val thirtyDaysAgo = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
    
    // This Month
    val thisMonthStart = (today.clone() as Calendar).apply { 
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
    
    // All time
    val allTimeStart = 0L

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentStart == sevenDaysAgo,
            onClick = { onRangeSelected(sevenDaysAgo, Long.MAX_VALUE) },
            label = { Text(stringResource(R.string.filter_7d)) }
        )
        FilterChip(
            selected = currentStart == thirtyDaysAgo,
            onClick = { onRangeSelected(thirtyDaysAgo, Long.MAX_VALUE) },
            label = { Text(stringResource(R.string.filter_30d)) }
        )
        FilterChip(
            selected = currentStart == thisMonthStart,
            onClick = { onRangeSelected(thisMonthStart, Long.MAX_VALUE) },
            label = { Text(stringResource(R.string.filter_month)) }
        )
        FilterChip(
            selected = currentStart == allTimeStart,
            onClick = { onRangeSelected(allTimeStart, Long.MAX_VALUE) },
            label = { Text(stringResource(R.string.filter_all)) }
        )
    }
}
