package com.billbuddy.android.ui.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbuddy.shared.auth.AuthenticationManager
import com.billbuddy.shared.groups.GroupRepository
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GroupListViewModel : ViewModel(), KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val authenticationManager: AuthenticationManager by inject()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUser: StateFlow<User?> = authenticationManager.currentUser

    init {
        // Observe current user changes to fetch groups
        currentUser.onEach { user ->
            if (user != null) {
                fetchGroupsForUser(user.id)
            } else {
                _groups.value = emptyList() // Clear groups if user logs out
            }
        }.launchIn(viewModelScope)
    }

    fun fetchGroupsForUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // This is a direct call. In a real app, GroupRepository might return Flow
                val userGroups = groupRepository.getGroupsForUser(userId)
                _groups.value = userGroups
            } catch (e: Exception) {
                _error.value = "Failed to fetch groups: ${e.message}"
                _groups.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Placeholder for creating a group - will navigate to a new screen or show dialog
    fun createNewGroup(name: String, creator: User, callback: (Group?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true // Indicate loading for create operation
            _error.value = null
            try {
                val newGroup = groupRepository.createGroup(name, creator)
                // Refresh list after creating (or rely on a Flow from repository if implemented)
                if (newGroup != null) {
                     val updatedGroups = groupRepository.getGroupsForUser(creator.id)
                    _groups.value = updatedGroups
                }
                callback(newGroup)
            } catch (e: Exception) {
                _error.value = "Failed to create group: ${e.message}"
                callback(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
