package com.example.finsight.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finsight.data.Budget
import com.example.finsight.databinding.ItemBudgetBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * RecyclerView Adapter for category budget list items with progress indicators and color-coded status.
 */
class BudgetAdapter(private val onItemClick: (Budget) -> Unit) :
    ListAdapter<BudgetItemState, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BudgetViewHolder(
        private val binding: ItemBudgetBinding,
        private val onItemClick: (Budget) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: BudgetItemState) {
            val budget = state.budget
            binding.root.setOnClickListener { onItemClick(budget) }

            binding.tvCategory.text = budget.category

            val spentStr = formatCurrency(state.spent)
            val limitStr = formatCurrency(budget.monthlyLimit)
            val percentStr = String.format(Locale.getDefault(), "%.1f%%", state.percentUsed)
            binding.tvSpentLimitPercent.text = "$spentStr / $limitStr ($percentStr)"

            // Color coding rules: <80% green, 80-99.9% orange, >=100% red
            val colorHex = when {
                state.percentUsed < 80f -> "#388E3C"
                state.percentUsed < 100f -> "#F57C00"
                else -> "#D32F2F"
            }
            val colorInt = Color.parseColor(colorHex)

            binding.tvSpentLimitPercent.setTextColor(colorInt)
            binding.progressBar.setIndicatorColor(colorInt)
            binding.progressBar.progress = state.percentUsed.toInt().coerceIn(0, 100)

            binding.tvRemaining.text = "Remaining: ${formatCurrency(state.remaining)}"

            if (state.percentUsed >= 100f && state.exceededBy > 0.0) {
                binding.tvExceededWarning.visibility = View.VISIBLE
                binding.tvExceededWarning.text = "Budget exceeded by ${formatCurrency(state.exceededBy)}"
            } else {
                binding.tvExceededWarning.visibility = View.GONE
            }
        }

        private fun formatCurrency(amount: Double): String {
            val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
            val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            val formattedNumber = formatter.format(abs(amount))
            return if (amount < 0) "- ₹$formattedNumber" else "₹$formattedNumber"
        }
    }

    private class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetItemState>() {
        override fun areItemsTheSame(oldItem: BudgetItemState, newItem: BudgetItemState): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: BudgetItemState, newItem: BudgetItemState): Boolean {
            return oldItem == newItem
        }
    }
}
