package com.example.finsight.ui

import android.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finsight.data.Transaction
import com.example.finsight.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for displaying transaction list items using ViewBinding.
 */
class TransactionAdapter :
    ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title

            val categoryPayment = if (transaction.isRecurring) {
                "${transaction.category} • ${transaction.paymentMethod} • Recurring"
            } else {
                "${transaction.category} • ${transaction.paymentMethod}"
            }
            binding.tvCategoryDetails.text = categoryPayment

            binding.tvDate.text = dateFormat.format(Date(transaction.date))

            val formattedAmount = if (transaction.isExpense) {
                "- ₹${String.format(Locale.getDefault(), "%.2f", transaction.amount)}"
            } else {
                "+ ₹${String.format(Locale.getDefault(), "%.2f", transaction.amount)}"
            }
            binding.tvAmount.text = formattedAmount

            val colorRes = if (transaction.isExpense) {
                R.color.holo_red_dark
            } else {
                R.color.holo_green_dark
            }
            binding.tvAmount.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
