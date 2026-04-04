package com.sans.expensetracker.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sans.expensetracker.R
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguageToggle: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val currentLanguage = viewModel.currentLanguage
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var tagToEdit by remember { mutableStateOf<TagEntity?>(null) }

    LaunchedEffect(viewModel.syncMessage) {
        viewModel.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.error) {
        viewModel.error?.let {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                            if (viewModel.isLoading) {
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
                    icon = getIconForName(category.icon),
                    onEdit = { categoryToEdit = category },
                    onDelete = { viewModel.deleteCategory(category) }
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
                    icon = Icons.Default.Label,
                    onEdit = { tagToEdit = tag },
                    onDelete = { viewModel.deleteTag(tag) }
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
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "category") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                val icons = listOf("restaurant", "health_and_safety", "shopping_bag", "commute", "language", "category")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    icons.forEach { iconName ->
                        val isSelected = icon == iconName
                        IconButton(
                            onClick = { icon = iconName },
                            modifier = Modifier.background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            )
                        ) {
                            Icon(getIconForName(iconName), contentDescription = null)
                        }
                    }
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
    var name by remember { mutableStateOf(tag.name) }

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

fun getIconForName(name: String): ImageVector {
    return when (name) {
        "restaurant" -> Icons.Default.Restaurant
        "health_and_safety" -> Icons.Default.HealthAndSafety
        "shopping_bag" -> Icons.Default.ShoppingBag
        "commute" -> Icons.Default.Commute
        "language" -> Icons.Default.Language
        "category" -> Icons.Default.Category
        else -> Icons.Default.Label
    }
}
