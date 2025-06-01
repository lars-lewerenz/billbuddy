package com.billbuddy.android.ui.grouplist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.billbuddy.android.ui.theme.BillBuddyTheme
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.User
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupListViewModel = koinViewModel(),
    onGroupClick: (groupId: String) -> Unit,
    // onNavigateToCreateGroup: () -> Unit, // Replaced by dialog logic
    onUserNotLoggedIn: () -> Unit // Callback if user becomes null
) {
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onUserNotLoggedIn()
        }
        // ViewModel's init block already fetches groups when currentUser changes.
        // No need to explicitly call fetchGroupsForUser here unless for a pull-to-refresh.
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Groups") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateGroupDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create New Group")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && groups.isEmpty()) { // Show loading only if groups are empty, otherwise show stale data
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (groups.isEmpty()) {
                Text(
                    text = "No groups yet. Tap the '+' button to create one!",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(groups, key = { group -> group.id }) { group ->
                        GroupListItem(group = group, onClick = { onGroupClick(group.id) })
                        Divider()
                    }
                }
            }

            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    onDismiss = { showCreateGroupDialog = false },
                    onCreateGroup = { groupName ->
                        currentUser?.let { user ->
                            viewModel.createNewGroup(groupName, user) { createdGroup ->
                                if (createdGroup != null) {
                                    // Optionally navigate to the new group or show success
                                    // For now, list will refresh due to viewModel logic
                                }
                                // Dialog will be dismissed by onDismiss
                            }
                        }
                        showCreateGroupDialog = false // Dismiss dialog after attempting create
                    }
                )
            }
        }
    }
}

@Composable
fun GroupListItem(group: Group, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(group.name) },
        supportingContent = { Text("Members: ${group.members.size} | Creator ID: ${group.createdBy.take(8)}...") }, // Showing first 8 chars of creator ID
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp) // Consistent padding
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (groupName: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(groupName)
                    }
                },
                enabled = groupName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GroupListScreenPreview() {
    BillBuddyTheme {
        // This preview will be basic as ViewModel interactions are complex.
        // For a more useful preview, you might pass mock data directly to a stateless version of GroupListScreenContent.
        GroupListScreen(
            onGroupClick = {},
            // onNavigateToCreateGroup = {}, // Removed as dialog is internal
            onUserNotLoggedIn = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListItemPreview() {
    BillBuddyTheme {
        GroupListItem(
            group = Group(
                id = "1",
                name = "Weekend Trip",
                members = listOf(
                    User("u1", "Alice", "alice@example.com"),
                    User("u2", "Bob", "bob@example.com")
                ),
                createdBy = "u1",
                inviteCode = "ABCDEF"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateGroupDialogPreview(){
    BillBuddyTheme {
        CreateGroupDialog(onDismiss = {}, onCreateGroup = {})
    }
}
