package com.example.finsight.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) for managing user budget database operations.
 */
@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY category ASC")
    fun getAllBudgets(userId: Long): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY category ASC")
    suspend fun getAllBudgetsOnce(userId: Long): List<Budget>
}
