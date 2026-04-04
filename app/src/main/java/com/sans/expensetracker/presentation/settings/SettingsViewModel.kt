package com.sans.expensetracker.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.TagEntity
import com.sans.expensetracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sans.expensetracker.data.local.AppDatabase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val localeManager: com.sans.expensetracker.data.util.LocaleManager,
    private val db: AppDatabase
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var syncMessage by mutableStateOf<String?>(null)
        private set

    val categories = repository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tags = repository.getAllTagEntities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var currentLanguage by mutableStateOf(localeManager.getLocale())
        private set

    fun updateLanguage(lang: String) {
        currentLanguage = lang
    }

    // Category CRUD
    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, icon = icon))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Tag CRUD
    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    fun exportFullBackup(context: android.content.Context) {
        isLoading = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.checkpoint()
                val dbName = "expense_tracker_db"
                val dbFile = context.getDatabasePath(dbName)
                
                if (!dbFile.exists()) {
                    error = "Database not found"
                    isLoading = false
                    return@launch
                }

                val snapshotName = "expense_tracker_db_snapshot.sqlite"
                val resolver = context.contentResolver
                val relativePath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/"
                
                val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(snapshotName)
                resolver.delete(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, snapshotName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it, "wt")?.use { outputStream ->
                        java.io.FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val done = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(it, done, null, null)
                    }
                    syncMessage = "Snapshot Saved: $snapshotName"
                    isLoading = false
                } ?: run {
                    error = "Failed to create snapshot"
                    isLoading = false
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    fun clearMessages() {
        error = null
        syncMessage = null
    }
}
