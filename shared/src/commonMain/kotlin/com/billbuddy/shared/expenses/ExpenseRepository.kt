package com.billbuddy.shared.expenses

import com.benasher44.uuid.uuid4
import com.billbuddy.shared.db.BillBuddyDatabase
import com.billbuddy.shared.model.Expense
import com.billbuddy.shared.model.Money
// import com.billbuddy.shared.model.User // Not directly used in this file but good for context
import kotlinx.datetime.Clock // For Clock.System.now()

class ExpenseRepository(private val database: BillBuddyDatabase) {

    fun addExpense(
        groupId: String,
        description: String,
        amount: Money,
        paidByUserId: String,
        participantUserIds: List<String>,
        date: Long = Clock.System.now().toEpochMilliseconds() // Default to now
    ): Expense? {
        val expenseId = uuid4().toString()

        // Start a transaction if your SQLDelight driver supports it and it's needed for atomicity
        // database.transaction { // Pseudo-code for transaction
        database.expenseQueries.insertExpense(
            id = expenseId,
            group_id = groupId,
            description = description,
            amount = amount,
            paid_by_user_id = paidByUserId,
            date = date
        )

        participantUserIds.forEach { userId ->
            database.expenseParticipantQueries.addParticipant(
                id = uuid4().toString(),
                expense_id = expenseId,
                user_id = userId
            )
        }
        // } // End transaction

        return getExpenseById(expenseId)
    }

    fun getExpenseById(expenseId: String): Expense? {
        val expenseEntity = database.expenseQueries.selectExpenseById(expenseId).executeAsOneOrNull() ?: return null
        val participantEntities = database.expenseParticipantQueries.getParticipantsForExpense(expenseId).executeAsList()

        return Expense(
            id = expenseEntity.id,
            groupId = expenseEntity.group_id,
            description = expenseEntity.description,
            amount = expenseEntity.amount, // Ensure this is Double
            paidBy = expenseEntity.paid_by_user_id,
            participants = participantEntities.map { it.user_id },
            date = expenseEntity.date
        )
    }

    fun getExpensesForGroup(groupId: String): List<Expense> {
        val expenseEntities = database.expenseQueries.selectExpensesByGroupId(groupId).executeAsList()
        return expenseEntities.mapNotNull { getExpenseById(it.id) } // Reuse to populate participants
    }

    fun getExpensesPaidByUser(userId: String, groupId: String? = null): List<Expense> {
        val expenseEntities = if (groupId != null) {
            database.expenseQueries.selectExpensesPaidByUserInGroup(userId, groupId).executeAsList()
        } else {
            database.expenseQueries.selectExpensesPaidByUser(userId).executeAsList()
        }
        return expenseEntities.mapNotNull { getExpenseById(it.id) }
    }

    fun getExpensesForUserParticipation(userId: String, groupId: String? = null): List<Expense> {
        val expenseEntities = if (groupId != null) {
            database.expenseQueries.selectExpensesForUserParticipationInGroup(userId, groupId).executeAsList()
        } else {
            database.expenseQueries.selectExpensesForUserParticipation(userId).executeAsList()
        }
        return expenseEntities.mapNotNull { getExpenseById(it.id) }
    }

    // Potentially methods to update or delete expenses
}
