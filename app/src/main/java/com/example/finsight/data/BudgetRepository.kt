package com.example.finsight.data

import androidx.lifecycle.LiveData

/**
 * Repository class providing a clean API for accessing user budget data.
 */
class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getAllBudgets(userId: Long): LiveData<List<Budget>> {
        return budgetDao.getAllBudgets(userId)
    }

    suspend fun getAllBudgetsOnce(userId: Long): List<Budget> {
        return budgetDao.getAllBudgetsOnce(userId)
    }

    suspend fun insertOrReplace(budget: Budget) {
        budgetDao.insertOrReplace(budget)
    }

    suspend fun delete(budget: Budget) {
        budgetDao.delete(budget)
    }
}
