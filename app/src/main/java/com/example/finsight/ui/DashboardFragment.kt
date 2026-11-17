package com.example.finsight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.finsight.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Fragment responsible for displaying summary metrics cards, category spending pie chart,
 * and spending insights.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private val monthDateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMonthTitle.text = monthDateFormat.format(Date())
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.dashboardSummary.observe(viewLifecycleOwner) { summary ->
            binding.tvOverallBalance.text = formatCurrency(summary.overallBalance)
            binding.tvMonthIncome.text = formatCurrency(summary.monthIncome)
            binding.tvMonthExpenses.text = formatCurrency(summary.monthExpenses)
            binding.tvMonthSavings.text = formatCurrency(summary.monthSavings)

            setupPieChart(summary.monthCategoryExpenses)
        }

        viewModel.insightTexts.observe(viewLifecycleOwner) { sentences ->
            val container = binding.llInsightsContainer
            container.removeAllViews()

            if (sentences.isEmpty()) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "Add some transactions to see insights"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                }
                container.addView(emptyTv)
            } else {
                for (sentence in sentences) {
                    val tv = TextView(requireContext()).apply {
                        text = "• $sentence"
                        textSize = 14f
                        setPadding(0, 4, 0, 8)
                    }
                    container.addView(tv)
                }
            }
        }
    }

    private fun setupPieChart(categoryExpenses: Map<String, Double>) {
        val pieChart: PieChart = binding.pieChart

        if (categoryExpenses.isEmpty() || categoryExpenses.values.sum() <= 0.0) {
            pieChart.isVisible = false
            binding.tvNoExpenses.isVisible = true
            return
        }

        pieChart.isVisible = true
        binding.tvNoExpenses.isVisible = false

        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryExpenses) {
            if (amount > 0.0) {
                entries.add(PieEntry(amount.toFloat(), category))
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.COLORFUL_COLORS.toList()
            valueTextSize = 13f
            valueTextColor = Color.WHITE
            sliceSpace = 3f
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }

        pieChart.apply {
            this.data = data
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(11f)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.isWordWrapEnabled = true
            animateY(800)
            invalidate()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
