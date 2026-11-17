package com.example.finsight.insights

import com.example.finsight.data.Budget
import com.example.finsight.data.Transaction
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Data class representing category spending comparison between current month and previous month.
 */
data class CategoryChange(
    val category: String,
    val thisMonthSpent: Double,
    val lastMonthSpent: Double,
    val percentChange: Double?,
    val isNew: Boolean
)

/**
 * Data class representing a budget warning when usage reaches or exceeds threshold.
 */
data class BudgetWarning(
    val category: String,
    val percentUsed: Float
)

/**
 * Pure Kotlin engine for computing financial insights, spending projections, and budget alerts.
 * Has no Android platform dependencies for easy unit testing.
 */
class InsightsEngine {

    /**
     * Calculates percentage change in expense per category comparing this month vs last month.
     */
    fun categoryChange(transactions: List<Transaction>, currentDate: Date): List<CategoryChange> {
        val (thisMonthExpenses, lastMonthExpenses) = getMonthExpensesByCategory(transactions, currentDate)

        val categories = (thisMonthExpenses.keys + lastMonthExpenses.keys).distinct()
        val changes = mutableListOf<CategoryChange>()

        for (category in categories) {
            val thisSpent = thisMonthExpenses[category] ?: 0.0
            val lastSpent = lastMonthExpenses[category] ?: 0.0

            if (thisSpent == 0.0 && lastSpent == 0.0) continue

            val isNew = lastSpent == 0.0 && thisSpent > 0.0
            val percentChange = if (lastSpent > 0.0) {
                ((thisSpent - lastSpent) / lastSpent) * 100.0
            } else null

            changes.add(
                CategoryChange(
                    category = category,
                    thisMonthSpent = thisSpent,
                    lastMonthSpent = lastSpent,
                    percentChange = percentChange,
                    isNew = isNew
                )
            )
        }

        return changes.sortedWith(
            compareByDescending<CategoryChange> { it.isNew }
                .thenByDescending { it.percentChange ?: 0.0 }
        )
    }

    /**
     * Calculates total expense change percentage comparing this month vs last month.
     */
    fun totalChange(transactions: List<Transaction>, currentDate: Date): Pair<Double, Double> {
        val (thisMonthExpenses, lastMonthExpenses) = getMonthExpensesByCategory(transactions, currentDate)
        val totalThis = thisMonthExpenses.values.sum()
        val totalLast = lastMonthExpenses.values.sum()
        return Pair(totalThis, totalLast)
    }

    /**
     * Returns the largest single expense transaction in the current calendar month.
     */
    fun largestExpense(transactions: List<Transaction>, currentDate: Date): Transaction? {
        val currentMonthTxs = getCurrentMonthExpenses(transactions, currentDate)
        return currentMonthTxs.maxByOrNull { it.amount }
    }

    /**
     * Returns the total sum of recurring expense transactions in the current month.
     */
    fun recurringTotal(transactions: List<Transaction>, currentDate: Date): Double {
        val currentMonthTxs = getCurrentMonthExpenses(transactions, currentDate)
        return currentMonthTxs.filter { it.isRecurring }.sumOf { it.amount }
    }

    /**
     * Projects total month-end expense based on days elapsed so far.
     */
    fun projectedMonthEnd(transactions: List<Transaction>, currentDate: Date): Double {
        val cal = Calendar.getInstance().apply { time = currentDate }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val daysElapsed = if (dayOfMonth <= 1) 1 else dayOfMonth
        val spentSoFar = getCurrentMonthExpenses(transactions, currentDate).sumOf { it.amount }

        return (spentSoFar / daysElapsed) * daysInMonth
    }

    /**
     * Calculates sum of excess spending in categories where this month's expense exceeds last month's.
     */
    fun potentialSavings(transactions: List<Transaction>, currentDate: Date): Double {
        val changes = categoryChange(transactions, currentDate)
        var totalPotentialSavings = 0.0
        for (change in changes) {
            if (change.thisMonthSpent > change.lastMonthSpent) {
                totalPotentialSavings += (change.thisMonthSpent - change.lastMonthSpent)
            }
        }
        return totalPotentialSavings
    }

    /**
     * Returns list of budget warnings for categories at 80% or more of their monthly limit.
     */
    fun budgetWarnings(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        currentDate: Date
    ): List<BudgetWarning> {
        val (thisMonthExpenses, _) = getMonthExpensesByCategory(transactions, currentDate)
        val warnings = mutableListOf<BudgetWarning>()

        for (budget in budgets) {
            val spent = thisMonthExpenses[budget.category] ?: 0.0
            if (budget.monthlyLimit > 0) {
                val percentUsed = ((spent / budget.monthlyLimit) * 100).toFloat()
                if (percentUsed >= 80f) {
                    warnings.add(BudgetWarning(budget.category, percentUsed))
                }
            }
        }

        return warnings
    }

    /**
     * Generates plain-English summary insight sentences.
     */
    fun generateInsightTexts(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        currentDate: Date
    ): List<String> {
        val insights = mutableListOf<String>()

        if (transactions.none { it.isExpense }) {
            return insights
        }

        val changes = categoryChange(transactions, currentDate)
        for (change in changes) {
            if (change.isNew) {
                insights.add("${change.category} is a new spending category this month (${formatCurrency(change.thisMonthSpent)}).")
            } else if (change.percentChange != null && change.percentChange > 0) {
                insights.add("${change.category} spending is up ${change.percentChange.toInt()}% vs last month.")
            } else if (change.percentChange != null && change.percentChange < 0) {
                val percentDown = abs(change.percentChange.toInt())
                insights.add("${change.category} spending is down $percentDown% vs last month.")
            }
        }

        val warnings = budgetWarnings(transactions, budgets, currentDate)
        for (warning in warnings) {
            insights.add("You've used ${warning.percentUsed.toInt()}% of your ${warning.category} budget.")
        }

        val projected = projectedMonthEnd(transactions, currentDate)
        if (projected > 0.0) {
            insights.add("At this pace you will spend ${formatCurrency(projected)} this month.")
        }

        val maxExpense = largestExpense(transactions, currentDate)
        if (maxExpense != null) {
            insights.add("Your largest expense this month is ${formatCurrency(maxExpense.amount)} on ${maxExpense.title}.")
        }

        val recurring = recurringTotal(transactions, currentDate)
        if (recurring > 0.0) {
            insights.add("Total recurring subscription expenses this month: ${formatCurrency(recurring)}.")
        }

        val potential = potentialSavings(transactions, currentDate)
        if (potential > 0.0) {
            insights.add("You could save up to ${formatCurrency(potential)} by matching last month's spending.")
        }

        return insights
    }

    private fun getMonthExpensesByCategory(
        transactions: List<Transaction>,
        currentDate: Date
    ): Pair<Map<String, Double>, Map<String, Double>> {
        val cal = Calendar.getInstance().apply { time = currentDate }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        cal.add(Calendar.MONTH, -1)
        val lastYear = cal.get(Calendar.YEAR)
        val lastMonth = cal.get(Calendar.MONTH)

        val thisMonthExpenses = mutableMapOf<String, Double>()
        val lastMonthExpenses = mutableMapOf<String, Double>()

        val txCal = Calendar.getInstance()
        for (tx in transactions) {
            if (!tx.isExpense) continue
            txCal.timeInMillis = tx.date
            val year = txCal.get(Calendar.YEAR)
            val month = txCal.get(Calendar.MONTH)

            if (year == currentYear && month == currentMonth) {
                thisMonthExpenses[tx.category] = (thisMonthExpenses[tx.category] ?: 0.0) + tx.amount
            } else if (year == lastYear && month == lastMonth) {
                lastMonthExpenses[tx.category] = (lastMonthExpenses[tx.category] ?: 0.0) + tx.amount
            }
        }

        return Pair(thisMonthExpenses, lastMonthExpenses)
    }

    private fun getCurrentMonthExpenses(
        transactions: List<Transaction>,
        currentDate: Date
    ): List<Transaction> {
        val cal = Calendar.getInstance().apply { time = currentDate }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        val txCal = Calendar.getInstance()
        return transactions.filter { tx ->
            if (!tx.isExpense) return@filter false
            txCal.timeInMillis = tx.date
            txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
        }
    }

    private fun formatCurrency(amount: Double): String {
        val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(abs(amount))
        return if (amount < 0) "- ₹$formattedNumber" else "₹$formattedNumber"
    }
}
