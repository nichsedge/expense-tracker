package com.sans.expensetracker.presentation.add_expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import com.sans.expensetracker.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate)
    var showDatePicker by androidx.compose.runtime.mutableStateOf(false)
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) stringResource(R.string.edit_transaction) else stringResource(R.string.add_expense), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Input with large text
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = { Text(stringResource(R.string.amount_spent)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            // Date Picker Field
            OutlinedTextField(
                value = dateFormatter.format(Date(viewModel.selectedDate)),
                onValueChange = { },
                label = { Text("Transaction Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.selectedDate = it
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = viewModel.categoryId == category.id,
                        onClick = { viewModel.categoryId = category.id },
                        label = { Text(category.name) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                }
            }

            Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val allTags by viewModel.allTags.collectAsState()

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tagsToShow = (allTags + viewModel.selectedTags).distinct()
                tagsToShow.forEach { tagName ->
                    FilterChip(
                        selected = viewModel.selectedTags.contains(tagName),
                        onClick = { viewModel.toggleTag(tagName) },
                        label = { Text(tagName) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.newTagText,
                onValueChange = { viewModel.newTagText = it },
                label = { Text("Add New Tag") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.addNewTag() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.addNewTag()
                        focusManager.clearFocus()
                    }
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = viewModel.itemName,
                onValueChange = { viewModel.itemName = it },
                label = { Text(stringResource(R.string.what_did_you_buy)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = viewModel.merchant,
                onValueChange = { viewModel.merchant = it },
                label = { Text(stringResource(R.string.merchant_store)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (viewModel.isInstallment) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (viewModel.isInstallment) focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) else focusManager.clearFocus() },
                    onDone = { 
                        focusManager.clearFocus() 
                        if (!viewModel.isInstallment) viewModel.onSaveClick(onBack)
                    }
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { viewModel.isInstallment = !viewModel.isInstallment }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.isInstallment,
                        onCheckedChange = { viewModel.isInstallment = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.is_installment),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (viewModel.isInstallment) {
                OutlinedTextField(
                    value = viewModel.durationMonths,
                    onValueChange = { viewModel.durationMonths = it },
                    label = { Text(stringResource(R.string.duration_months)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus() 
                            viewModel.onSaveClick(onBack)
                        }
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.onSaveClick(onBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(if (viewModel.isEditMode) stringResource(R.string.update_transaction) else stringResource(R.string.confirm_transaction), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
