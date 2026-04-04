package com.sans.expensetracker.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sans.expensetracker.R
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.monthly_summary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                
                StatsComparisonCard(
                    title = stringResource(R.string.this_month),
                    amount = state.thisMonthSpent,
                    currencyFormat = currencyFormat
                )
                
                StatsComparisonCard(
                    title = stringResource(R.string.last_month),
                    amount = state.lastMonthSpent,
                    currencyFormat = currencyFormat,
                    isSecondary = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.yearly_summary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                StatsComparisonCard(
                    title = stringResource(R.string.this_year),
                    amount = state.thisYearSpent,
                    currencyFormat = currencyFormat
                )

                StatsComparisonCard(
                    title = stringResource(R.string.last_year),
                    amount = state.lastYearSpent,
                    currencyFormat = currencyFormat,
                    isSecondary = true
                )
            }
        }
    }
}

@Composable
fun StatsComparisonCard(
    title: String,
    amount: Long,
    currencyFormat: NumberFormat,
    isSecondary: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSecondary) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSecondary)
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                currencyFormat.format(amount / 100.0).replace(",00", ""),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSecondary)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
