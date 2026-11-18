package com.example.finsight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a financial transaction (income or expense) belonging to a specific user.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["userId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val paymentMethod: String,
    val date: Long,
    val isRecurring: Boolean = false
)
