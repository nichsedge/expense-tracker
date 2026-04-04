package com.sans.expensetracker.presentation.expense_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.sans.expensetracker.R
import com.sans.expensetracker.domain.model.Expense
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpenseClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onLanguageToggle: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.syncMessage) {
        state.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

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
                    IconButton(onClick = { viewModel.exportFullBackup(context) }) {
                        Icon(Icons.Default.Sync, contentDescription = "Full Sync Data")
                    }
                    IconButton(onClick = onLanguageToggle) {
                        Icon(Icons.Default.Translate, contentDescription = stringResource(R.string.language))
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onInstallmentsClick) {
                        Icon(Icons.Default.List, contentDescription = "Installment Plans")
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    expenseToDelete = expense
                                    showDeleteDialog = true
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) {
                            ExpenseItem(
                                expense = expense,
                                onClick = { onExpenseClick(expense.id) }
                            )
                        }
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
}

@Composable
fun SummaryCard(thisMonth: Long, periodTotal: Long) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    
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
                        currencyFormat.format(thisMonth / 100.0).replace(",00", ""),
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
                        currencyFormat.format(periodTotal / 100.0).replace(",00", ""),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onClick: () -> Unit) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    expense.merchant ?: stringResource(R.string.personal_expense),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(expense.amount / 100.0).replace(",00", ""),
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
