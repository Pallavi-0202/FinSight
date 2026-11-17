package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.Budget
import com.example.finsight.data.BudgetRepository
import com.example.finsight.data.Transaction
import com.example.finsight.data.TransactionRepository
import com.example.finsight.insights.InsightsEngine
import java.util.Calendar
import java.util.Date

/**
 * Data class holding calculated dashboard statistics for summary cards and chart.
 */
data class DashboardSummary(
    val overallBalance: Double,
    val monthIncome: Double,
    val monthExpenses: Double,
    val monthSavings: Double,
    val monthCategoryExpenses: Map<String, Double>
)

/**
 * ViewModel responsible for transforming transaction data into financial overview summary metrics
 * and spending insights.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val budgetRepository: BudgetRepository
    private val insightsEngine = InsightsEngine()

    val dashboardSummary = MediatorLiveData<DashboardSummary>()
    val insightTexts = MediatorLiveData<List<String>>()

    init {
        val db = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        budgetRepository = BudgetRepository(db.budgetDao())

        dashboardSummary.addSource(transactionRepository.allTransactions) { transactions ->
            dashboardSummary.value = calculateSummary(transactions)
            updateInsights(transactions, budgetRepository.allBudgets.value.orEmpty())
        }

        insightTexts.addSource(budgetRepository.allBudgets) { budgets ->
            updateInsights(transactionRepository.allTransactions.value.orEmpty(), budgets)
        }
    }

    private fun updateInsights(transactions: List<Transaction>, budgets: List<Budget>) {
        val currentDate = Date()
        insightTexts.value = insightsEngine.generateInsightTexts(transactions, budgets, currentDate)
    }

    private fun calculateSummary(transactions: List<Transaction>): DashboardSummary {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        var totalAllTimeIncome = 0.0
        var totalAllTimeExpense = 0.0

        var monthIncome = 0.0
        var monthExpenses = 0.0
        val categoryExpensesMap = mutableMapOf<String, Double>()

        val txCalendar = Calendar.getInstance()

        for (transaction in transactions) {
            if (transaction.isExpense) {
                totalAllTimeExpense += transaction.amount
            } else {
                totalAllTimeIncome += transaction.amount
            }

            txCalendar.timeInMillis = transaction.date
            val isCurrentMonth = txCalendar.get(Calendar.YEAR) == currentYear &&
                    txCalendar.get(Calendar.MONTH) == currentMonth

            if (isCurrentMonth) {
                if (transaction.isExpense) {
                    monthExpenses += transaction.amount
                    val currentCatExpense = categoryExpensesMap.getOrDefault(transaction.category, 0.0)
                    categoryExpensesMap[transaction.category] = currentCatExpense + transaction.amount
                } else {
                    monthIncome += transaction.amount
                }
            }
        }

        val overallBalance = totalAllTimeIncome - totalAllTimeExpense
        val monthSavings = monthIncome - monthExpenses

        return DashboardSummary(
            overallBalance = overallBalance,
            monthIncome = monthIncome,
            monthExpenses = monthExpenses,
            monthSavings = monthSavings,
            monthCategoryExpenses = categoryExpensesMap
        )
    }
}
