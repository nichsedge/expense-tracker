package com.sans.expensetracker.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.expensetracker.R
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.TagEntity
import com.sans.expensetracker.presentation.components.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguageToggle: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val currentLanguage = viewModel.currentLanguage.value
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var tagToEdit by remember { mutableStateOf<TagEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var tagToDelete by remember { mutableStateOf<TagEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    val currentBudget by viewModel.monthlyBudget.collectAsState()

    LaunchedEffect(viewModel.syncMessage.value) {
        viewModel.syncMessage.value?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.error.value) {
        viewModel.error.value?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Data Management Section
            item {
                SettingsSectionTitle(stringResource(R.string.data_management))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.exportFullBackup(context) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.isLoading.value) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.full_backup),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.backup_to_downloads),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            // Language Section
            item {
                SettingsSectionTitle(stringResource(R.string.language))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLanguageToggle() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Text(if (currentLanguage == "en") "English" else "Indonesia", style = MaterialTheme.typography.bodyLarge)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                SettingsSectionTitle("Features")
                Surface(
                    onClick = onNavigateToRecurring,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.recurring_expenses), modifier = Modifier.weight(1f))
                    }
                }

                Surface(
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly Budget")
                            Text(
                                if (currentBudget > 0L) com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(currentBudget) else "Not Set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionTitle(stringResource(R.string.categories))
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_category))
                    }
                }
            }

            items(categories) { category ->
                SettingsItem(
                    title = category.name,
                    icon = category.icon,
                    onEdit = { categoryToEdit = category },
                    onDelete = { categoryToDelete = category }
                )
            }

            // Tags Section
            item {
                SettingsSectionTitle(stringResource(R.string.tags))
            }

            if (tags.isEmpty()) {
                item {
                    Text(
                        "No tags found. Tags are created when you add them to expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            items(tags) { tag ->
                SettingsItem(
                    title = tag.name,
                    icon = "🏷️",
                    onEdit = { tagToEdit = tag },
                    onDelete = { tagToDelete = tag }
                )
            }

        }
    }

    // Dialogs
    if (showAddCategoryDialog) {
        CategoryEditDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, icon ->
                viewModel.addCategory(name, icon)
                showAddCategoryDialog = false
            }
        )
    }

    categoryToEdit?.let { category ->
        CategoryEditDialog(
            category = category,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, icon ->
                viewModel.updateCategory(category.copy(name = name, icon = icon))
                categoryToEdit = null
            }
        )
    }

    tagToEdit?.let { tag ->
        TagEditDialog(
            tag = tag,
            onDismiss = { tagToEdit = null },
            onConfirm = { newName ->
                viewModel.updateTag(tag.copy(name = newName))
                tagToEdit = null
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete category '${category.name}'? All expenses in this category will become uncategorized.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete tag '${tag.name}'? This will remove the tag from all associated expenses.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(if (currentBudget > 0L) (currentBudget / 100).toString() else "")
        }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newBudget = budgetInput.toLongOrNull()?.times(100) ?: 0L
                    viewModel.updateMonthlyBudget(newBudget)
                    showBudgetDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                icon = icon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(category?.id) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                    
                    val presets = listOf("🍔", "💊", "🛍️", "🚗", "🌐", "📁", "🏠", "🎮", "🎁", "💡", "💰", "🔧")
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { emoji ->
                            val isSelected = icon == emoji
                            Surface(
                                onClick = { icon = emoji },
                                shape = MaterialTheme.shapes.small,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { if (it.length <= 2) icon = it },
                        label = { Text("Custom Emoji") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, icon) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TagEditDialog(
    tag: TagEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(tag.id) { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.lowercase().trim()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
