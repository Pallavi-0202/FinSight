package com.example.finsight.data

import androidx.lifecycle.LiveData

/**
 * Repository class providing a clean API for accessing user transaction data.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(userId: Long): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions(userId)
    }

    suspend fun getAllTransactionsOnce(userId: Long): List<Transaction> {
        return transactionDao.getAllTransactionsOnce(userId)
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

    suspend fun getTransactionsBetweenDates(userId: Long, startDate: Long, endDate: Long): List<Transaction> {
        return transactionDao.getTransactionsBetweenDates(userId, startDate, endDate)
    }
}
