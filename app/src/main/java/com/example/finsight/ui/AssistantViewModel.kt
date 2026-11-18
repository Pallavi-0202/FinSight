package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finsight.ai.ChatMessage
import com.example.finsight.ai.FinanceContextBuilder
import com.example.finsight.ai.GeminiClient
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.BudgetRepository
import com.example.finsight.data.TransactionRepository
import kotlinx.coroutines.launch

const val SYSTEM_INSTRUCTION = "You are FinSight, a friendly personal finance assistant for a student in India. Answer ONLY from the financial data provided. If the data does not contain the answer, say so. Never invent numbers. Use ₹. Keep answers under 120 words. Give practical, simple advice."

/**
 * ViewModel for managing chat interaction with Gemini AI assistant and maintaining conversation state.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val budgetRepository: BudgetRepository
    private val geminiClient = GeminiClient()
    private val contextBuilder = FinanceContextBuilder()

    private val historyList = mutableListOf<ChatMessage>()
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> get() = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val db = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        budgetRepository = BudgetRepository(db.budgetDao())
    }

    fun sendMessage(userQuery: String) {
        val trimmedQuery = userQuery.trim()
        if (trimmedQuery.isEmpty()) return

        val userMessage = ChatMessage(trimmedQuery, isUser = true)
        historyList.add(userMessage)

        if (historyList.size > 6) {
            historyList.removeAt(0)
        }
        _messages.value = historyList.toList()

        _isLoading.value = true

        viewModelScope.launch {
            val transactions = transactionRepository.allTransactions.value.orEmpty()
            val budgets = budgetRepository.allBudgets.value.orEmpty()

            val financeData = contextBuilder.buildContext(transactions, budgets)
            val fullInstruction = "$SYSTEM_INSTRUCTION\n\nFinancial Data:\n$financeData"

            val historyForApi = if (historyList.size > 1) {
                historyList.subList(0, historyList.size - 1)
            } else {
                emptyList()
            }

            val result = geminiClient.generate(fullInstruction, historyForApi, trimmedQuery)

            _isLoading.value = false

            result.onSuccess { responseText ->
                val assistantMessage = ChatMessage(responseText, isUser = false)
                historyList.add(assistantMessage)
                if (historyList.size > 6) {
                    historyList.removeAt(0)
                }
                _messages.value = historyList.toList()
            }.onFailure { error ->
                val errorMessage = ChatMessage(error.message ?: "Failed to get AI response.", isUser = false)
                val currentListWithErr = historyList.toList() + errorMessage
                _messages.value = currentListWithErr
            }
        }
    }
}
