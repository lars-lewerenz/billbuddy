package com.billbuddy.shared.splitting

import com.billbuddy.shared.expenses.ExpenseRepository
import com.billbuddy.shared.groups.GroupRepository
import com.billbuddy.shared.model.Expense
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.Money
import com.billbuddy.shared.model.SplitBalance
import com.billbuddy.shared.model.SplitSummary
import com.billbuddy.shared.model.User
import com.billbuddy.shared.model.UserDebt

// Add repositories to the constructor
class ExpenseSplitterService(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository
) {

    // New method that fetches data using repositories
    fun calculateSplitForGroup(groupId: String): SplitSummary? {
        val group = groupRepository.getGroupById(groupId) ?: return null // Or throw exception
        val expenses = expenseRepository.getExpensesForGroup(groupId)
        return calculateSplitInternal(group, expenses)
    }

    // Keep the original logic in a private/internal method if direct passing of data is still useful for tests
    // Or rename the original public method to something like calculateSplitWithProvidedData
    internal fun calculateSplitInternal(group: Group, expenses: List<Expense>): SplitSummary {
        if (group.members.isEmpty()) {
            return SplitSummary(emptyList(), emptyList())
        }

        // Step 1: Calculate how much each user paid in total
        val totalPaidByUser = mutableMapOf<String, Money>() // Key: UserID, Value: Amount Paid
        for (expense in expenses) {
            totalPaidByUser[expense.paidBy] = (totalPaidByUser[expense.paidBy] ?: 0.0) + expense.amount
        }

        // Step 2: Calculate how much each user *should have* paid (their share)
        val totalShareByUser = mutableMapOf<String, Money>() // Key: UserID, Value: Total Share
        for (expense in expenses) {
            if (expense.participants.isEmpty()) continue // Skip expenses with no participants

            val costPerParticipant = expense.amount / expense.participants.size
            for (participantId in expense.participants) {
                totalShareByUser[participantId] = (totalShareByUser[participantId] ?: 0.0) + costPerParticipant
            }
        }

        // Step 3: Determine the net balance for each member
        val netBalances = group.members.map { member ->
            val paid = totalPaidByUser[member.id] ?: 0.0
            val shared = totalShareByUser[member.id] ?: 0.0
            SplitBalance(user = member, netBalance = paid - shared)
        }

        // Step 4: Simplify debts and recommend transactions
        val recommendedTransactions = generateRecommendedTransactions(netBalances) // Renamed back for clarity

        return SplitSummary(balances = netBalances, recommendedTransactions = recommendedTransactions)
    }

    // Renamed back from "추천거래생성" for better readability and maintainability
    private fun generateRecommendedTransactions(balances: List<SplitBalance>): List<UserDebt> {
        val transactions = mutableListOf<UserDebt>()
        if (balances.isEmpty()) return transactions

        // Operate on copies of balance data to avoid modifying the input list if it's not intended
        val debtors = balances.filter { it.netBalance < -0.00001 }.map { it.copy() }.toMutableList()
        val creditors = balances.filter { it.netBalance > 0.00001 }.map { it.copy() }.toMutableList()


        debtors.sortBy { it.netBalance }
        creditors.sortByDescending { it.netBalance }

        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val currentDebtor = debtors[debtorIndex]
            val currentCreditor = creditors[creditorIndex]

            val amountDebtorOwes = -currentDebtor.netBalance
            val amountCreditorIsOwed = currentCreditor.netBalance

            val transferAmount = minOf(amountDebtorOwes, amountCreditorIsOwed)

            if (transferAmount > 0.00001) {
                transactions.add(
                    UserDebt(
                        fromUser = currentDebtor.user,
                        toUser = currentCreditor.user,
                        amount = transferAmount
                    )
                )
                // Update balances for the next iteration (important to use the mutable list copies)
                debtors[debtorIndex] = currentDebtor.copy(netBalance = currentDebtor.netBalance + transferAmount)
                creditors[creditorIndex] = currentCreditor.copy(netBalance = currentCreditor.netBalance - transferAmount)
            }

            if ((-debtors[debtorIndex].netBalance) < 0.00001) { // Debtor settled
                debtorIndex++
            }
            if (creditors[creditorIndex].netBalance < 0.00001) { // Creditor settled
                creditorIndex++
            }
        }
        return transactions
    }
}
