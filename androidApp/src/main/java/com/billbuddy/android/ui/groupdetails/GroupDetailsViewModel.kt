package com.billbuddy.android.ui.groupdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbuddy.shared.groups.GroupRepository
import com.billbuddy.shared.expenses.ExpenseRepository
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.Expense
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GroupDetailsViewModel(savedStateHandle: SavedStateHandle) : ViewModel(), KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val expenseRepository: ExpenseRepository by inject()

    val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (groupId.isNotBlank()) {
            loadGroupDetails()
        } else {
            _error.value = "Group ID is missing."
        }
    }

    fun loadGroupDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _group.value = groupRepository.getGroupById(groupId)
                _expenses.value = expenseRepository.getExpensesForGroup(groupId)
            } catch (e: Exception) {
                _error.value = "Failed to load group details: ${e.message}"
                _group.value = null
                _expenses.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
