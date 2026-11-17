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
import com.example.finsight.databinding.ItemTopBudgetBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Fragment responsible for displaying summary metrics cards, category spending pie chart,
 * 6-month trend bar chart, top budget status, and spending insights.
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

        viewModel.monthlyTrends.observe(viewLifecycleOwner) { trends ->
            setupBarChart(binding.barChart, trends)
        }

        viewModel.topBudgets.observe(viewLifecycleOwner) { topBudgets ->
            setupTopBudgets(topBudgets)
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

    private fun setupBarChart(barChart: BarChart, trends: List<MonthlyTrend>) {
        if (trends.isEmpty()) return

        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for (i in trends.indices) {
            val trend = trends[i]
            incomeEntries.add(BarEntry(i.toFloat(), trend.income.toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), trend.expense.toFloat()))
            labels.add(trend.monthLabel)
        }

        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = Color.parseColor("#388E3C")
            valueTextSize = 9f
        }
        val expenseDataSet = BarDataSet(expenseEntries, "Expenses").apply {
            color = Color.parseColor("#D32F2F")
            valueTextSize = 9f
        }

        val groupSpace = 0.2f
        val barSpace = 0.05f
        val barWidth = 0.35f

        val data = BarData(incomeDataSet, expenseDataSet).apply {
            this.barWidth = barWidth
        }

        barChart.apply {
            this.data = data
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = trends.size.toFloat()
                setDrawGridLines(false)
            }

            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT

            groupBars(0f, groupSpace, barSpace)
            animateY(800)
            invalidate()
        }
    }

    private fun setupTopBudgets(topBudgets: List<TopBudgetItem>) {
        val container = binding.llTopBudgetsContainer
        container.removeAllViews()

        if (topBudgets.isEmpty()) {
            binding.tvNoBudgets.isVisible = true
            container.isVisible = false
            return
        }

        binding.tvNoBudgets.isVisible = false
        container.isVisible = true

        for (item in topBudgets) {
            val itemBinding = ItemTopBudgetBinding.inflate(layoutInflater, container, false)

            itemBinding.tvCategory.text = item.category
            itemBinding.tvSpentLimit.text = "${formatCurrency(item.spent)} / ${formatCurrency(item.limit)}"

            val colorHex = when {
                item.percentUsed < 80f -> "#388E3C"
                item.percentUsed < 100f -> "#F57C00"
                else -> "#D32F2F"
            }
            val colorInt = Color.parseColor(colorHex)

            itemBinding.progressBar.setIndicatorColor(colorInt)
            itemBinding.progressBar.progress = item.percentUsed.toInt().coerceIn(0, 100)

            container.addView(itemBinding.root)
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
