package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.Transaction
import com.example.finsight.data.TransactionRepository
import java.util.Calendar

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
 * ViewModel responsible for transforming transaction data into financial overview summary metrics.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val dashboardSummary: LiveData<DashboardSummary>

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)

        dashboardSummary = repository.allTransactions.map { transactions ->
            calculateSummary(transactions)
        }
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
