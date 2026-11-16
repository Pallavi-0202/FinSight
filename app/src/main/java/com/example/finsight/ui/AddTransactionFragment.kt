package com.example.finsight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.finsight.R
import com.example.finsight.data.Transaction
import com.example.finsight.databinding.FragmentAddTransactionBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment for creating a new financial transaction with input validation.
 */
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()

    private val categories = arrayOf(
        "Food", "Travel", "Shopping", "Bills",
        "Entertainment", "Health", "Salary", "Other"
    )
    private val paymentMethods = arrayOf("Cash", "UPI", "Card")

    private var selectedDateEpochMillis: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupDatePicker()
        setupSaveButton()
    }

    private fun setupDropdowns() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(categoryAdapter)
        binding.actvCategory.setText(categories[0], false)

        val paymentAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            paymentMethods
        )
        binding.actvPaymentMethod.setAdapter(paymentAdapter)
        binding.actvPaymentMethod.setText(paymentMethods[1], false)
    }

    private fun setupDatePicker() {
        updateDateText()

        val datePickerListener = View.OnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Transaction Date")
                .setSelection(selectedDateEpochMillis)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDateEpochMillis = selection
                updateDateText()
            }

            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        binding.etDate.setOnClickListener(datePickerListener)
        binding.tilDate.setOnClickListener(datePickerListener)
    }

    private fun updateDateText() {
        binding.etDate.setText(dateFormat.format(Date(selectedDateEpochMillis)))
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val amountString = binding.etAmount.text?.toString()?.trim().orEmpty()
        val amountParsed = amountString.toDoubleOrNull()
        val category = binding.actvCategory.text?.toString()?.trim()?.ifEmpty { categories[0] } ?: categories[0]
        val paymentMethod = binding.actvPaymentMethod.text?.toString()?.trim()?.ifEmpty { paymentMethods[0] } ?: paymentMethods[0]
        val isExpense = binding.toggleType.checkedButtonId != R.id.btn_income
        val isRecurring = binding.cbRecurring.isChecked

        var isValid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        if (amountParsed == null || amountParsed <= 0.0) {
            binding.tilAmount.error = "Enter a valid amount greater than 0"
            isValid = false
        } else {
            binding.tilAmount.error = null
        }

        if (!isValid || amountParsed == null) return

        val transaction = Transaction(
            title = title,
            amount = amountParsed,
            isExpense = isExpense,
            category = category,
            paymentMethod = paymentMethod,
            date = selectedDateEpochMillis,
            isRecurring = isRecurring
        )

        viewModel.addTransaction(transaction)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
