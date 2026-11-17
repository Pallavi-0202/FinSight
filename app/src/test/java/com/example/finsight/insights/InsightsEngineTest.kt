package com.example.finsight.insights

import com.example.finsight.data.Budget
import com.example.finsight.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit tests for InsightsEngine analytics and calculation logic using JUnit 4.
 */
class InsightsEngineTest {

    private val engine = InsightsEngine()

    private val midMonthDate: Date = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
    }.time

    private val dayOneDate: Date = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
    }.time

    private fun novDate(day: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, day, 10, 0, 0)
        }.timeInMillis
    }

    private fun octDate(day: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, day, 10, 0, 0)
        }.timeInMillis
    }

    @Test
    fun testCategoryChange_NormalValues() {
        val txs = listOf(
            Transaction(title = "Grocery 1", amount = 1000.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = octDate(10)),
            Transaction(title = "Grocery 2", amount = 1500.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = novDate(10))
        )

        val changes = engine.categoryChange(txs, midMonthDate)

        assertEquals(1, changes.size)
        val foodChange = changes[0]
        assertEquals("Food", foodChange.category)
        assertEquals(50.0, foodChange.percentChange!!, 0.01)
        assertEquals(false, foodChange.isNew)
    }

    @Test
    fun testCategoryChange_LastMonthZero_IsNew() {
        val txs = listOf(
            Transaction(title = "Flight", amount = 5000.0, isExpense = true, category = "Travel", paymentMethod = "Card", date = novDate(5))
        )

        val changes = engine.categoryChange(txs, midMonthDate)

        assertEquals(1, changes.size)
        val travelChange = changes[0]
        assertEquals("Travel", travelChange.category)
        assertNull(travelChange.percentChange)
        assertTrue(travelChange.isNew)
    }

    @Test
    fun testProjectedMonthEnd_DayOneAndMidMonth() {
        val dayOneTxs = listOf(
            Transaction(title = "Coffee", amount = 100.0, isExpense = true, category = "Food", paymentMethod = "Cash", date = novDate(1))
        )
        val projDayOne = engine.projectedMonthEnd(dayOneTxs, dayOneDate)
        assertEquals(3000.0, projDayOne, 0.01)

        val midMonthTxs = listOf(
            Transaction(title = "Dinner", amount = 1500.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = novDate(10))
        )
        val projMidMonth = engine.projectedMonthEnd(midMonthTxs, midMonthDate)
        assertEquals(3000.0, projMidMonth, 0.01)
    }

    @Test
    fun testBudgetWarningThreshold_79PercentNo_80PercentYes() {
        val budgets = listOf(
            Budget(id = 1, category = "Food", monthlyLimit = 1000.0),
            Budget(id = 2, category = "Travel", monthlyLimit = 1000.0)
        )

        val txs = listOf(
            Transaction(title = "Food tx", amount = 790.0, isExpense = true, category = "Food", paymentMethod = "UPI", date = novDate(5)),
            Transaction(title = "Travel tx", amount = 800.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = novDate(5))
        )

        val warnings = engine.budgetWarnings(txs, budgets, midMonthDate)

        assertEquals(1, warnings.size)
        assertEquals("Travel", warnings[0].category)
        assertEquals(80.0f, warnings[0].percentUsed, 0.01f)
    }

    @Test
    fun testLargestExpense() {
        val txs = listOf(
            Transaction(title = "Snack", amount = 50.0, isExpense = true, category = "Food", paymentMethod = "Cash", date = novDate(2)),
            Transaction(title = "Laptop", amount = 45000.0, isExpense = true, category = "Shopping", paymentMethod = "Card", date = novDate(10)),
            Transaction(title = "Taxi", amount = 300.0, isExpense = true, category = "Travel", paymentMethod = "UPI", date = novDate(12))
        )

        val largest = engine.largestExpense(txs, midMonthDate)

        assertNotNull(largest)
        assertEquals("Laptop", largest!!.title)
        assertEquals(45000.0, largest.amount, 0.01)
    }
}
