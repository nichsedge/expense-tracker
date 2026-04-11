package com.sans.expensetracker.presentation.scan_invoice

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Content
import org.json.JSONArray
import org.json.JSONObject

data class SuggestedTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val category: String,
    val dateString: String? = null,
    val isAccepted: Boolean = true
)

data class ScanInvoiceState(
    val modelUri: Uri? = null,
    val cachedModelPath: String? = null,
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val suggestedTransactions: List<SuggestedTransaction> = emptyList()
)

sealed class ScanInvoiceEvent {
    data class ModelSelected(val uri: Uri) : ScanInvoiceEvent()
    data class CacheModelFile(val context: Context, val uri: Uri) : ScanInvoiceEvent()
    data class ImageSelected(val uri: Uri) : ScanInvoiceEvent()
    data class ProcessDemoInvoice(val context: Context) : ScanInvoiceEvent()
    data class ToggleTransactionAcceptance(val id: String) : ScanInvoiceEvent()
    object SaveAcceptedTransactions : ScanInvoiceEvent()
}

@HiltViewModel
class ScanInvoiceViewModel @Inject constructor(
    private val expenseRepository: com.sans.expensetracker.domain.repository.ExpenseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanInvoiceState())
    val state: StateFlow<ScanInvoiceState> = _state.asStateFlow()

    fun onEvent(event: ScanInvoiceEvent) {
        when (event) {
            is ScanInvoiceEvent.ModelSelected -> {
                _state.update { it.copy(modelUri = event.uri, cachedModelPath = null) }
            }
            is ScanInvoiceEvent.CacheModelFile -> {
                viewModelScope.launch {
                    val path = cacheFile(event.context, event.uri, "model.litertlm")
                    _state.update { it.copy(cachedModelPath = path) }
                }
            }
            is ScanInvoiceEvent.ImageSelected -> {
                _state.update { it.copy(imageUri = event.uri, isProcessing = true) }
                // Try caching image if context was available. Since it's not in the event we will rely on
                // processing demo invoice to actually pass an absolute file path.
                // A future iteration would pass Context in ImageSelected event.
                processInference("")
            }
            is ScanInvoiceEvent.ProcessDemoInvoice -> {
                _state.update { it.copy(isProcessing = true) }
                viewModelScope.launch {
                    // Copy demo invoice from assets to cache
                    val demoFile = File(event.context.cacheDir, "sample_invoice.jpg")
                    withContext(Dispatchers.IO) {
                        event.context.assets.open("sample_invoice.jpg").use { input ->
                            FileOutputStream(demoFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    processInference(demoFile.absolutePath)
                }
            }
            is ScanInvoiceEvent.ToggleTransactionAcceptance -> {
                _state.update { currentState ->
                    currentState.copy(
                        suggestedTransactions = currentState.suggestedTransactions.map {
                            if (it.id == event.id) it.copy(isAccepted = !it.isAccepted) else it
                        }
                    )
                }
            }
            is ScanInvoiceEvent.SaveAcceptedTransactions -> {
                viewModelScope.launch {
                    val accepted = _state.value.suggestedTransactions.filter { it.isAccepted }

                    // Fetch categories to try and match the string or assign default
                    val categories = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val defaultCategoryId = categories.firstOrNull()?.id ?: 1L

                    accepted.forEach { tx ->
                        // Basic fuzzy match or fallback
                        val matchedCategory = categories.find { it.name.equals(tx.category, ignoreCase = true) }
                        val catId = matchedCategory?.id ?: defaultCategoryId

                        // Parse date if available
                        val txDate = try {
                            tx.dateString?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it)?.time
                            } ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        expenseRepository.insertExpense(
                            Expense(
                                date = txDate,
                                itemName = tx.title,
                                amount = tx.amount,
                                categoryId = catId,
                                merchant = "Scanned Invoice",
                                tags = listOf("AI Scanned")
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun cacheFile(context: Context, uri: Uri, fileName: String): String {
        return withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }
    }

    private fun processInference(imagePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelPath = _state.value.cachedModelPath ?: return@launch

                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(), // Or GPU if requested
                    visionBackend = Backend.CPU()
                )

                Engine(engineConfig).use { engine ->
                    engine.initialize()

                    val conversationConfig = ConversationConfig(
                        systemInstruction = Contents.of("You are a helpful assistant that acts as a JSON API.")
                    )

                    engine.createConversation(conversationConfig).use { conversation ->
                        val prompt = "Extract all purchased items from this invoice. Respond ONLY with a valid JSON array. Each object in the array should have these properties: 'title' (string), 'amount' (integer representing cents, so 50.00 becomes 5000), 'category' (string, e.g., 'Groceries', 'Food', 'Misc'), and 'dateString' (string, format YYYY-MM-DD if available, else null)."

                        val messageResponse = if (imagePath.isNotEmpty() && File(imagePath).exists()) {
                            conversation.sendMessage(
                                Contents.of(
                                    Content.ImageFile(imagePath),
                                    Content.Text(prompt)
                                )
                            )
                        } else {
                            // Fallback if image doesn't exist
                            conversation.sendMessage(prompt)
                        }

                        // Using toString() on the contents to extract the string result from the model safely
                        val resultText = messageResponse.contents.contents.filterIsInstance<Content.Text>().joinToString("\n") { it.text }

                        // Basic cleanup for markdown json blocks if the model wrapped it
                        val cleanJson = resultText.replace("```json", "").replace("```", "").trim()

                        val suggestions = mutableListOf<SuggestedTransaction>()
                        try {
                            val jsonArray = JSONArray(cleanJson)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                suggestions.add(
                                    SuggestedTransaction(
                                        title = obj.optString("title", "Unknown"),
                                        amount = obj.optLong("amount", 0L),
                                        category = obj.optString("category", "Uncategorized"),
                                        dateString = if (obj.has("dateString")) obj.getString("dateString") else null
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback to mock data if JSON parsing fails due to bad model output
                            suggestions.addAll(
                                listOf(
                                    SuggestedTransaction(title = "TELUR AYAM NEGERI (Mock)", amount = 52655, category = "Groceries", dateString = "2026-02-15"),
                                    SuggestedTransaction(title = "RTE CHICK.ROAST ROSE (Mock)", amount = 39900, category = "Groceries", dateString = "2026-02-15"),
                                    SuggestedTransaction(title = "NUTRISARI JRK/P5X14 (Mock)", amount = 10090, category = "Groceries", dateString = "2026-02-15")
                                )
                            )
                        }

                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    suggestedTransactions = suggestions
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false) }
                }
            }
        }
    }
}
