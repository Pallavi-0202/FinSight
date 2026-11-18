package com.example.finsight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.finsight.auth.SessionManager
import com.example.finsight.data.AppDatabase
import com.example.finsight.data.Transaction
import com.example.finsight.data.TransactionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing UI data related to user-specific financial transactions.
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val sessionManager = SessionManager(application)
    val currentUserId: Long = sessionManager.currentUserId()

    val allTransactions: LiveData<List<Transaction>>

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        allTransactions = repository.getAllTransactions(currentUserId)
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val userTx = if (transaction.userId == 0L) {
                transaction.copy(userId = currentUserId)
            } else {
                transaction
            }
            repository.insert(userTx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }
}
