package com.example.finsight.data

import androidx.lifecycle.LiveData

/**
 * Repository class providing a clean API for accessing budget data.
 */
class BudgetRepository(private val budgetDao: BudgetDao) {

    val allBudgets: LiveData<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun getAllBudgetsOnce(): List<Budget> {
        return budgetDao.getAllBudgetsOnce()
    }

    suspend fun insertOrReplace(budget: Budget) {
        budgetDao.insertOrReplace(budget)
    }

    suspend fun delete(budget: Budget) {
        budgetDao.delete(budget)
    }
}
