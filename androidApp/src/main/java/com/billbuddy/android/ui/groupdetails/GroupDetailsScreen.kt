package com.billbuddy.android.ui.groupdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.billbuddy.android.ui.theme.BillBuddyTheme
import com.billbuddy.shared.model.Expense
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.User
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupId: String, // Passed via navigation
    // Use koinViewModel and pass initial parameters if needed by SavedStateHandle
    viewModel: GroupDetailsViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: (groupId: String) -> Unit,
    onNavigateToSplitDetails: (groupId: String) -> Unit
) {
    val group by viewModel.group.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // This LaunchedEffect ensures that if groupId changes (e.g. due to deep link or recomposition with new args),
    // data is reloaded. However, with SavedStateHandle, ViewModel should persist across config changes.
    // ViewModel's init block already loads data if groupId is present.
    // This might be redundant if ViewModel correctly uses SavedStateHandle for initial load.
    LaunchedEffect(viewModel.groupId) { // Observe the groupId from the ViewModel itself which is from SavedStateHandle
        if (viewModel.groupId.isNotBlank()) {
            // viewModel.loadGroupDetails() // ViewModel's init already does this. Only call if refresh is needed.
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                   Button(onClick = { onNavigateToSplitDetails(groupId) }) {
                       Text("Split")
                   }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddExpense(groupId) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (group == null) {
                Text(
                    text = "Group not found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                GroupDetailsContent(group!!, expenses, onExpenseClick = { /* Maybe view expense details? */})
            }
        }
    }
}

@Composable
fun GroupDetailsContent(group: Group, expenses: List<Expense>, onExpenseClick: (expenseId: String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Group Name: ${group.name}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Invite Code: ${group.inviteCode ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Members:", style = MaterialTheme.typography.titleMedium)
            group.members.forEach { member ->
                Text("- ${member.displayName ?: member.email ?: member.id.take(8)+"..."}")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Expenses:", style = MaterialTheme.typography.titleMedium)
        }

        if (expenses.isEmpty()) {
            item {
                Text("No expenses yet. Add one!")
            }
        } else {
            items(expenses, key = { expense -> expense.id }) { expense ->
                ExpenseListItem(expense = expense, groupMembers = group.members, onClick = { onExpenseClick(expense.id) })
                Divider()
            }
        }
    }
}

@Composable
fun ExpenseListItem(expense: Expense, groupMembers: List<User>, onClick: () -> Unit) {
    val payer = groupMembers.find { it.id == expense.paidBy }
    ListItem(
        headlineContent = { Text(expense.description) },
        supportingContent = {
            Text("Amount: ${"%.2f".format(expense.amount)} | Paid by: ${payer?.displayName ?: expense.paidBy.take(8)+"..."}")
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    )
}


@Preview(showBackground = true)
@Composable
fun GroupDetailsScreenPreview() {
    BillBuddyTheme {
        val sampleUser = User("id1", "Preview User", "preview@example.com")
        val sampleGroup = Group("groupPrev", "Preview Group", listOf(sampleUser), sampleUser.id, "PREV123")
        val sampleExpenses = listOf(
            Expense("expPrev1", "groupPrev", "Lunch", 50.0, sampleUser.id, listOf(sampleUser.id), System.currentTimeMillis())
        )
        // This preview is static. ViewModel interaction is not live.
        // For a more interactive preview, use a fake ViewModel that provides this data.
        // A simple way for preview when ViewModel needs SavedStateHandle:
        class PreviewGroupDetailsViewModel : GroupDetailsViewModel(SavedStateHandle(mapOf("groupId" to "groupPrev"))) {
            init {
                // Manually set data for preview
                // _group.value = sampleGroup
                // _expenses.value = sampleExpenses
            }
            // Override methods if needed for preview interaction
        }


        GroupDetailsScreen(
            groupId = "groupPrev",
            // viewModel = PreviewGroupDetailsViewModel(), // This would be ideal for full preview
            onNavigateBack = {},
            onNavigateToAddExpense = {},
            onNavigateToSplitDetails = {}
        )
    }
}
