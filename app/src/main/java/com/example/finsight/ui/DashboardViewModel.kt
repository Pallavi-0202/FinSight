package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finsight.ai.FinanceContextBuilder
import com.example.finsight.ai.GeminiClient
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.Budget
import com.example.finsight.data.BudgetRepository
import com.example.finsight.data.Transaction
import com.example.finsight.data.TransactionRepository
import com.example.finsight.insights.InsightsEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
 * Data class representing total income and expenses for a single calendar month.
 */
data class MonthlyTrend(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

/**
 * Data class representing a category budget item with usage status for top budget display.
 */
data class TopBudgetItem(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentUsed: Float
)

/**
 * ViewModel responsible for transforming transaction data into financial overview summary metrics,
 * 6-month trends, top budget alerts, and spending insights.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val budgetRepository: BudgetRepository
    private val insightsEngine = InsightsEngine()
    private val geminiClient = GeminiClient()
    private val contextBuilder = FinanceContextBuilder()

    val dashboardSummary = MediatorLiveData<DashboardSummary>()
    val insightTexts = MediatorLiveData<List<String>>()
    val monthlyTrends = MediatorLiveData<List<MonthlyTrend>>()
    val topBudgets = MediatorLiveData<List<TopBudgetItem>>()

    private val _aiExplanation = MutableLiveData<String>()
    val aiExplanation: LiveData<String> get() = _aiExplanation

    private val _isExplainLoading = MutableLiveData<Boolean>(false)
    val isExplainLoading: LiveData<Boolean> get() = _isExplainLoading

    init {
        val db = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        budgetRepository = BudgetRepository(db.budgetDao())

        dashboardSummary.addSource(transactionRepository.allTransactions) { transactions ->
            dashboardSummary.value = calculateSummary(transactions)
            monthlyTrends.value = calculateMonthlyTrends(transactions)
            updateInsights(transactions, budgetRepository.allBudgets.value.orEmpty())
            topBudgets.value = calculateTopBudgets(
                budgetRepository.allBudgets.value.orEmpty(),
                transactions
            )
        }

        insightTexts.addSource(budgetRepository.allBudgets) { budgets ->
            val transactions = transactionRepository.allTransactions.value.orEmpty()
            updateInsights(transactions, budgets)
            topBudgets.value = calculateTopBudgets(budgets, transactions)
        }
    }

    fun explainMyMonth() {
        _isExplainLoading.value = true

        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactionsOnce()
            val budgets = budgetRepository.getAllBudgetsOnce()

            val financeData = contextBuilder.buildContext(transactions, budgets)
            val prompt = "Provide a short plain-language summary of my financial performance this month and 2 practical saving tips."
            val fullInstruction = "$SYSTEM_INSTRUCTION\n\nFinancial Data:\n$financeData"

            val result = geminiClient.generate(fullInstruction, emptyList(), prompt)
            _isExplainLoading.value = false

            result.onSuccess { text ->
                _aiExplanation.value = text
            }.onFailure { error ->
                _aiExplanation.value = error.message ?: "Failed to explain month."
            }
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

    private fun calculateMonthlyTrends(transactions: List<Transaction>): List<MonthlyTrend> {
        val trends = mutableListOf<MonthlyTrend>()
        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val monthCalendars = mutableListOf<Calendar>()
        for (i in 5 downTo 0) {
            val mCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            monthCalendars.add(mCal)
        }

        val txCal = Calendar.getInstance()

        for (mCal in monthCalendars) {
            val year = mCal.get(Calendar.YEAR)
            val month = mCal.get(Calendar.MONTH)
            val label = dateFormat.format(mCal.time)

            var monthIncome = 0.0
            var monthExpense = 0.0

            for (tx in transactions) {
                txCal.timeInMillis = tx.date
                if (txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month) {
                    if (tx.isExpense) {
                        monthExpense += tx.amount
                    } else {
                        monthIncome += tx.amount
                    }
                }
            }

            trends.add(MonthlyTrend(label, monthIncome, monthExpense))
        }

        return trends
    }

    private fun calculateTopBudgets(
        budgets: List<Budget>,
        transactions: List<Transaction>
    ): List<TopBudgetItem> {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

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

        return budgets.map { budget ->
            val spent = categoryExpenses.getOrDefault(budget.category, 0.0)
            val percentUsed = if (budget.monthlyLimit > 0) {
                ((spent / budget.monthlyLimit) * 100).toFloat()
            } else 0f

            TopBudgetItem(
                category = budget.category,
                spent = spent,
                limit = budget.monthlyLimit,
                percentUsed = percentUsed
            )
        }.sortedByDescending { it.percentUsed }.take(3)
    }
}
