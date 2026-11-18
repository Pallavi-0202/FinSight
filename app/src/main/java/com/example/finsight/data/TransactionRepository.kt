package com.example.finsight.data

import androidx.lifecycle.LiveData

/**
 * Repository class providing a clean API for accessing transaction data.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun getAllTransactionsOnce(): List<Transaction> {
        return transactionDao.getAllTransactionsOnce()
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getTransactionsBetweenDates(startDate: Long, endDate: Long): List<Transaction> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }
}
