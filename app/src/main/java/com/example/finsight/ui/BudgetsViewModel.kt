package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.Budget
import com.example.finsight.data.BudgetRepository
import com.example.finsight.data.Transaction
import com.example.finsight.data.TransactionRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * UI State representation of a category budget combined with current month's expenses.
 */
data class BudgetItemState(
    val budget: Budget,
    val spent: Double,
    val percentUsed: Float,
    val remaining: Double,
    val exceededBy: Double
)

/**
 * ViewModel for managing category budget limits and monitoring spending against budgets.
 */
class BudgetsViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetRepository: BudgetRepository
    private val transactionRepository: TransactionRepository

    val budgetItems = MediatorLiveData<List<BudgetItemState>>()

    init {
        val db = AppDatabase.getDatabase(application)
        budgetRepository = BudgetRepository(db.budgetDao())
        transactionRepository = TransactionRepository(db.transactionDao())

        budgetItems.addSource(budgetRepository.allBudgets) { budgets ->
            combineBudgetsAndTransactions(budgets, transactionRepository.allTransactions.value.orEmpty())
        }

        budgetItems.addSource(transactionRepository.allTransactions) { transactions ->
            combineBudgetsAndTransactions(budgetRepository.allBudgets.value.orEmpty(), transactions)
        }
    }

    private fun combineBudgetsAndTransactions(
        budgets: List<Budget>,
        transactions: List<Transaction>
    ) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val categoryExpenses = mutableMapOf<String, Double>()
        val txCal = Calendar.getInstance()

        for (tx in transactions) {
            if (tx.isExpense) {
                txCal.timeInMillis = tx.date
                if (txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth) {
                    val currentSpent = categoryExpenses.getOrDefault(tx.category, 0.0)
                    categoryExpenses[tx.category] = currentSpent + tx.amount
                }
            }
        }

        val items = budgets.map { budget ->
            val spent = categoryExpenses.getOrDefault(budget.category, 0.0)
            val percentUsed = if (budget.monthlyLimit > 0) {
                ((spent / budget.monthlyLimit) * 100).toFloat()
            } else 0f
            val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)
            val exceededBy = if (spent > budget.monthlyLimit) spent - budget.monthlyLimit else 0.0

            BudgetItemState(
                budget = budget,
                spent = spent,
                percentUsed = percentUsed,
                remaining = remaining,
                exceededBy = exceededBy
            )
        }

        budgetItems.value = items
    }

    fun addOrUpdateBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.insertOrReplace(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.delete(budget)
        }
    }
}
