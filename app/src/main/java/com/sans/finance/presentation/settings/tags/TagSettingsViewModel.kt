package com.sans.finance.presentation.settings.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.TagEntity
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagSettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val tags = repository.getAllTagEntities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun onTagsReordered(reorderedTags: List<TagEntity>) {
        viewModelScope.launch {
            val updatedTags = reorderedTags.mapIndexed { index, tag ->
                tag.copy(orderIndex = index)
            }
            repository.updateTags(updatedTags)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }
}
