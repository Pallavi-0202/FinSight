package com.example.finsight.ai

import android.util.Log
import com.example.finsight.data.Budget
import com.example.finsight.data.Transaction
import com.example.finsight.insights.InsightsEngine
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Helper class for constructing detailed financial context text from real user data for AI prompts.
 */
class FinanceContextBuilder {

    private val insightsEngine = InsightsEngine()

    fun buildContext(
        transactions: List<Transaction>,
        budgets: List<Budget>
    ): String {
        val currentDate = Date()
        val calendar = Calendar.getInstance().apply { time = currentDate }
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        var monthIncome = 0.0
        var monthExpenses = 0.0
        val categoryExpensesMap = mutableMapOf<String, Double>()

        val txCalendar = Calendar.getInstance()
        for (tx in transactions) {
            if (tx.isExpense) {
                txCalendar.timeInMillis = tx.date
                if (txCalendar.get(Calendar.YEAR) == currentYear && txCalendar.get(Calendar.MONTH) == currentMonth) {
                    monthExpenses += tx.amount
                    categoryExpensesMap[tx.category] = (categoryExpensesMap[tx.category] ?: 0.0) + tx.amount
                }
            } else {
                txCalendar.timeInMillis = tx.date
                if (txCalendar.get(Calendar.YEAR) == currentYear && txCalendar.get(Calendar.MONTH) == currentMonth) {
                    monthIncome += tx.amount
                }
            }
        }

        val monthSavings = monthIncome - monthExpenses
        val sb = StringBuilder()

        sb.append("--- CURRENT MONTH FINANCIAL SUMMARY ---\n")
        sb.append("Income: ").append(formatCurrency(monthIncome)).append("\n")
        sb.append("Expenses: ").append(formatCurrency(monthExpenses)).append("\n")
        sb.append("Savings: ").append(formatCurrency(monthSavings)).append("\n\n")

        sb.append("--- SPENDING BY CATEGORY ---\n")
        if (categoryExpensesMap.isEmpty()) {
            sb.append("No expenses recorded for this month.\n")
        } else {
            for ((category, amount) in categoryExpensesMap) {
                sb.append("- ").append(category).append(": ").append(formatCurrency(amount)).append("\n")
            }
        }
        sb.append("\n")

        sb.append("--- BUDGET STATUS ---\n")
        if (budgets.isEmpty()) {
            sb.append("No monthly budgets configured.\n")
        } else {
            for (b in budgets) {
                val spent = categoryExpensesMap[b.category] ?: 0.0
                val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit * 100).toInt() else 0
                sb.append("- ").append(b.category).append(": Spent ")
                    .append(formatCurrency(spent)).append(" of ").append(formatCurrency(b.monthlyLimit))
                    .append(" (").append(pct).append("%)\n")
            }
        }
        sb.append("\n")

        val insightSentences = insightsEngine.generateInsightTexts(transactions, budgets, currentDate)
        sb.append("--- AUTOMATED INSIGHTS ---\n")
        if (insightSentences.isEmpty()) {
            sb.append("None.\n")
        } else {
            for (sentence in insightSentences) {
                sb.append("• ").append(sentence).append("\n")
            }
        }
        sb.append("\n")

        val recentTxs = transactions.take(30)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sb.append("--- RECENT TRANSACTIONS (Top 30) ---\n")
        if (recentTxs.isEmpty()) {
            sb.append("No transaction history available.\n")
        } else {
            for (tx in recentTxs) {
                val typeStr = if (tx.isExpense) "Expense" else "Income"
                val amountStr = if (tx.isExpense) "- ${formatCurrency(tx.amount)}" else "+ ${formatCurrency(tx.amount)}"
                sb.append(dateFormat.format(Date(tx.date))).append(" | ")
                    .append(tx.title).append(" (").append(tx.category).append(", ").append(typeStr).append("): ")
                    .append(amountStr).append("\n")
            }
        }

        val contextText = sb.toString()
        Log.d("FinSightContext", contextText)
        return contextText
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
