package com.billbuddy.shared.splitting

import com.billbuddy.shared.db.BillBuddyDatabase
import com.billbuddy.shared.db.ExpenseQueries
import com.billbuddy.shared.db.ExpenseParticipantQueries
import com.billbuddy.shared.db.GroupQueries
import com.billbuddy.shared.db.GroupMemberQueries
import com.billbuddy.shared.db.UserQueries
import com.billbuddy.shared.expenses.ExpenseRepository
import com.billbuddy.shared.groups.GroupRepository
import com.billbuddy.shared.model.Expense
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExpenseSplitterServiceTest {

    // Minimal Fake Repositories for testing the splitter service in isolation
    // These are not used by the current tests as we call calculateSplitInternal directly for most tests.
    // They would be needed if we were testing calculateSplitForGroup.
    // For now, providing simple stubs that return empty/null or throw TODO().
    class FakeGroupRepository : GroupRepository(FakeDb().instance) {
        private val groups = mutableMapOf<String, Group>()
        fun addGroup(group: Group) { groups[group.id] = group }
        override fun getGroupById(groupId: String): Group? = groups[groupId]
        // Other methods can be stubbed as needed if calculateSplitForGroup is tested more deeply
    }

    class FakeExpenseRepository : ExpenseRepository(FakeDb().instance) {
        private val expenses = mutableMapOf<String, MutableList<Expense>>()
        fun addExpense(expense: Expense) {
            expenses.getOrPut(expense.groupId) { mutableListOf() }.add(expense)
        }
        override fun getExpensesForGroup(groupId: String): List<Expense> = expenses[groupId] ?: emptyList()
        // Other methods can be stubbed
    }

    // Fake Database for Repository instantiation
    class FakeDb {
        // A very basic mock/stub for BillBuddyDatabase and its queries
        // In a real scenario, you might use a mocking framework or more detailed fakes.
        private val mockExpenseQueries: ExpenseQueries = TODO("Stub or mock ExpenseQueries if methods are called")
        private val mockExpenseParticipantQueries: ExpenseParticipantQueries = TODO("Stub or mock ExpenseParticipantQueries")
        private val mockGroupQueries: GroupQueries = TODO("Stub or mock GroupQueries")
        private val mockGroupMemberQueries: GroupMemberQueries = TODO("Stub or mock GroupMemberQueries")
        private val mockUserQueries: UserQueries = TODO("Stub or mock UserQueries")

        val instance: BillBuddyDatabase = object : BillBuddyDatabase {
            override fun close() {}
            override val expenseQueries: ExpenseQueries get() = mockExpenseQueries
            override val expenseParticipantQueries: ExpenseParticipantQueries get() = mockExpenseParticipantQueries
            override val groupQueries: GroupQueries get() = mockGroupQueries
            override val groupMemberQueries: GroupMemberQueries get() = mockGroupMemberQueries
            override val userQueries: UserQueries get() = mockUserQueries
        }
    }

    private val fakeGroupRepository = FakeGroupRepository()
    private val fakeExpenseRepository = FakeExpenseRepository()
    private val splitterService = ExpenseSplitterService(fakeGroupRepository, fakeExpenseRepository)

    // Test Users
    private val userA = User("userA", "Alice", "alice@example.com")
    private val userB = User("userB", "Bob", "bob@example.com")
    private val userC = User("userC", "Charlie", "charlie@example.com")
    // private val userD = User("userD", "David", "david@example.com") // Not used in current tests


    @Test
    fun testSimpleEvenSplitTwoUsers() {
        val group = Group("group1", "Test Group", listOf(userA, userB), userA.id, "INVITE1")
        val expenses = listOf(
            Expense("exp1", "group1", "Dinner", 100.0, userA.id, listOf(userA.id, userB.id), 0L)
        )
        // Directly call calculateSplitInternal for focused unit testing of the logic
        val summary = splitterService.calculateSplitInternal(group, expenses)

        assertNotNull(summary)
        assertEquals(2, summary.balances.size)
        assertEquals(1, summary.recommendedTransactions.size)

        val balanceA = summary.balances.first { it.user.id == userA.id }
        val balanceB = summary.balances.first { it.user.id == userB.id }

        assertEquals(50.0, balanceA.netBalance, 0.001) // Alice paid 100, share was 50. Owed 50.
        assertEquals(-50.0, balanceB.netBalance, 0.001) // Bob paid 0, share was 50. Owes 50.

        val transaction = summary.recommendedTransactions.first()
        assertEquals(userB.id, transaction.fromUser.id)
        assertEquals(userA.id, transaction.toUser.id)
        assertEquals(50.0, transaction.amount, 0.001)
    }

    @Test
    fun testOneUserPaysAllThreeParticipate() {
        val group = Group("group2", "Test Group 2", listOf(userA, userB, userC), userA.id, "INVITE2")
        val expenses = listOf(
            Expense("exp2", "group2", "Groceries", 90.0, userA.id, listOf(userA.id, userB.id, userC.id), 0L)
        )
        val summary = splitterService.calculateSplitInternal(group, expenses)

        assertNotNull(summary)
        val balanceA = summary.balances.first { it.user.id == userA.id } // Paid 90, share 30. Owed 60.
        val balanceB = summary.balances.first { it.user.id == userB.id } // Paid 0, share 30. Owes 30.
        val balanceC = summary.balances.first { it.user.id == userC.id } // Paid 0, share 30. Owes 30.

        assertEquals(60.0, balanceA.netBalance, 0.001)
        assertEquals(-30.0, balanceB.netBalance, 0.001)
        assertEquals(-30.0, balanceC.netBalance, 0.001)

        assertEquals(2, summary.recommendedTransactions.size) // B->A, C->A
        assertTrue(summary.recommendedTransactions.any { it.fromUser.id == userB.id && it.toUser.id == userA.id && it.amount == 30.0 })
        assertTrue(summary.recommendedTransactions.any { it.fromUser.id == userC.id && it.toUser.id == userA.id && it.amount == 30.0 })
    }

    @Test
    fun testMultipleExpensesDifferentPayers() {
        val group = Group("group3", "Test Group 3", listOf(userA, userB), userA.id, "INVITE3")
        val expenses = listOf(
            Expense("exp3a", "group3", "Lunch", 40.0, userA.id, listOf(userA.id, userB.id), 0L), // A pays 40, share 20. A: +20
            Expense("exp3b", "group3", "Tickets", 60.0, userB.id, listOf(userA.id, userB.id), 0L) // B pays 60, share 30. B: +30
        )
        // A: paid 40, share (20+30)=50. Net -10 (owes 10)
        // B: paid 60, share (20+30)=50. Net +10 (owed 10)
        val summary = splitterService.calculateSplitInternal(group, expenses)

        assertNotNull(summary)
        val balanceA = summary.balances.first { it.user.id == userA.id }
        val balanceB = summary.balances.first { it.user.id == userB.id }

        assertEquals(-10.0, balanceA.netBalance, 0.001)
        assertEquals(10.0, balanceB.netBalance, 0.001)

        assertEquals(1, summary.recommendedTransactions.size)
        val transaction = summary.recommendedTransactions.first()
        assertEquals(userA.id, transaction.fromUser.id)
        assertEquals(userB.id, transaction.toUser.id)
        assertEquals(10.0, transaction.amount, 0.001)
    }

    @Test
    fun testNoExpenses() {
        val group = Group("group4", "Test Group 4", listOf(userA, userB), userA.id, "INVITE4")
        val expenses = emptyList<Expense>()
        val summary = splitterService.calculateSplitInternal(group, expenses)

        assertNotNull(summary)
        summary.balances.forEach { assertEquals(0.0, it.netBalance, 0.001) }
        assertTrue(summary.recommendedTransactions.isEmpty())
    }

    @Test
    fun testExpenseWithOnlyOneParticipant() {
        val group = Group("group5", "Test Group 5", listOf(userA, userB), userA.id, "INVITE5")
        val expenses = listOf(
            Expense("exp5", "group5", "Solo Item", 50.0, userA.id, listOf(userA.id), 0L) // A pays 50 for A. Net 0 for A.
        )
        // A: paid 50, share 50. Net 0.
        // B: paid 0, share 0. Net 0.
        val summary = splitterService.calculateSplitInternal(group, expenses)

        assertNotNull(summary)
        summary.balances.forEach { assertEquals(0.0, it.netBalance, 0.001) }
        assertTrue(summary.recommendedTransactions.isEmpty())
    }

    @Test
    fun testComplexScenarioThreeUsers() {
        val group = Group("group6", "Complex Group", listOf(userA, userB, userC), userA.id, "INVITE6")
        val expenses = listOf(
            Expense("e1", "group6", "Dinner", 150.0, userA.id, listOf(userA.id, userB.id, userC.id), 0L), // A pays 150. Each share 50. A:+100, B:-50, C:-50
            Expense("e2", "group6", "Movie", 30.0, userB.id, listOf(userA.id, userB.id), 0L),        // B pays 30. A,B share 15. B:+15, A:-15. (C not involved)
            Expense("e3", "group6", "Coffee", 15.0, userC.id, listOf(userA.id, userC.id), 0L)       // C pays 15. A,C share 7.5. C:+7.5, A:-7.5
        )

        // Totals:
        // User A: Paid 150. Share (50 from e1) + (15 from e2) + (7.5 from e3) = 72.5. Net: 150 - 72.5 = +77.5
        // User B: Paid 30.  Share (50 from e1) + (15 from e2) = 65. Net: 30 - 65 = -35
        // User C: Paid 15.  Share (50 from e1) + (7.5 from e3) = 57.5. Net: 15 - 57.5 = -42.5

        val summary = splitterService.calculateSplitInternal(group, expenses)
        assertNotNull(summary)

        val balanceA = summary.balances.first { it.user.id == userA.id }
        val balanceB = summary.balances.first { it.user.id == userB.id }
        val balanceC = summary.balances.first { it.user.id == userC.id }

        assertEquals(77.5, balanceA.netBalance, 0.001)
        assertEquals(-35.0, balanceB.netBalance, 0.001)
        assertEquals(-42.5, balanceC.netBalance, 0.001)

        assertEquals(2, summary.recommendedTransactions.size) // B->A (35), C->A (42.5)
        // Order of transactions might vary, so check amounts and participants
        val transactionForB = summary.recommendedTransactions.firstOrNull { it.fromUser.id == userB.id }
        assertNotNull(transactionForB)
        assertEquals(userA.id, transactionForB.toUser.id)
        assertEquals(35.0, transactionForB.amount, 0.001)

        val transactionForC = summary.recommendedTransactions.firstOrNull { it.fromUser.id == userC.id }
        assertNotNull(transactionForC)
        assertEquals(userA.id, transactionForC.toUser.id)
        assertEquals(42.5, transactionForC.amount, 0.001)
    }

    @Test
    fun testSettledDebtsNoTransactions() {
        val group = Group("group7", "Settled Group", listOf(userA, userB), userA.id, "INVITE7")
        val expensesCorrected = listOf(
            Expense("exp7a", "group7", "A's Item", 10.0, userA.id, listOf(userA.id), 0L),
            Expense("exp7b", "group7", "B's Item", 10.0, userB.id, listOf(userB.id), 0L)
        )

        val summary = splitterService.calculateSplitInternal(group, expensesCorrected)
        assertNotNull(summary)
        summary.balances.forEach { assertEquals(0.0, it.netBalance, 0.001) }
        assertTrue(summary.recommendedTransactions.isEmpty())
    }

    // Example test for calculateSplitForGroup (would require more setup for Fake Repositories)
    @Test
    fun testCalculateSplitForGroup_SimpleScenario() {
        val groupId = "groupRepoTest"
        val group = Group(groupId, "Repo Test Group", listOf(userA, userB), userA.id, "INVITEREP")
        val expense = Expense("expRepo", groupId, "Repo Dinner", 100.0, userA.id, listOf(userA.id, userB.id), 0L)

        (fakeGroupRepository as FakeGroupRepository).addGroup(group) // Assumes addGroup method in Fake
        (fakeExpenseRepository as FakeExpenseRepository).addExpense(expense) // Assumes addExpense method in Fake

        val summary = splitterService.calculateSplitForGroup(groupId)

        assertNotNull(summary)
        assertEquals(2, summary.balances.size)
        assertEquals(1, summary.recommendedTransactions.size)

        val balanceA = summary.balances.first { it.user.id == userA.id }
        assertEquals(50.0, balanceA.netBalance, 0.001)
        val transaction = summary.recommendedTransactions.first()
        assertEquals(userB.id, transaction.fromUser.id)
        assertEquals(userA.id, transaction.toUser.id)
        assertEquals(50.0, transaction.amount, 0.001)
    }
}
