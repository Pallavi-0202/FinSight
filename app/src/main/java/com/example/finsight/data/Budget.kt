package com.example.finsight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a monthly category budget limit.
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val monthlyLimit: Double
)
