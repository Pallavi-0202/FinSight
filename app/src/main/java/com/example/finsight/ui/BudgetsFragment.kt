package com.example.finsight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finsight.R
import com.example.finsight.data.Budget
import com.example.finsight.databinding.DialogAddEditBudgetBinding
import com.example.finsight.databinding.FragmentBudgetsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment for displaying, creating, editing, and deleting category budget limits.
 */
class BudgetsFragment : Fragment() {

    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetsViewModel by viewModels()
    private lateinit var adapter: BudgetAdapter

    private val categories = arrayOf(
        "Food", "Travel", "Shopping", "Bills",
        "Entertainment", "Health", "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter { budgetToEdit ->
            showAddEditBudgetDialog(budgetToEdit)
        }
        binding.rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudgets.adapter = adapter

        // Swipe-to-delete handling
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val budgetToDelete = adapter.currentList[position].budget

                viewModel.deleteBudget(budgetToDelete)

                val bottomNav = requireActivity().findViewById<View>(R.id.bottom_nav)
                val snackbar = Snackbar.make(binding.root, "Budget deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        viewModel.addOrUpdateBudget(budgetToDelete)
                    }

                if (bottomNav != null) {
                    snackbar.anchorView = bottomNav
                } else {
                    snackbar.anchorView = binding.fabAddBudget
                }
                snackbar.show()
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvBudgets)
    }

    private fun setupFab() {
        binding.fabAddBudget.setOnClickListener {
            showAddEditBudgetDialog()
        }
    }

    private fun showAddEditBudgetDialog(existingBudget: Budget? = null) {
        val dialogBinding = DialogAddEditBudgetBinding.inflate(layoutInflater)

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        dialogBinding.actvCategory.setAdapter(categoryAdapter)

        if (existingBudget != null) {
            dialogBinding.actvCategory.setText(existingBudget.category, false)
            dialogBinding.etLimit.setText(existingBudget.monthlyLimit.toString())
        } else {
            dialogBinding.actvCategory.setText(categories[0], false)
        }

        val titleText = if (existingBudget != null) "Edit Budget" else "Add Budget"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleText)
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedCategory = dialogBinding.actvCategory.text?.toString()?.trim().orEmpty()
            val limitString = dialogBinding.etLimit.text?.toString()?.trim().orEmpty()
            val limit = limitString.toDoubleOrNull()

            if (limit == null || limit <= 0.0) {
                dialogBinding.tilLimit.error = "Enter a valid limit greater than 0"
                return@setOnClickListener
            }

            dialogBinding.tilLimit.error = null

            val categoryToSave = if (selectedCategory.isNotEmpty()) selectedCategory else categories[0]

            val newBudget = Budget(
                id = existingBudget?.id ?: 0,
                category = categoryToSave,
                monthlyLimit = limit
            )

            viewModel.addOrUpdateBudget(newBudget)
            dialog.dismiss()
        }
    }

    private fun observeViewModel() {
        viewModel.budgetItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvEmptyBudgets.isVisible = items.isEmpty()
            binding.rvBudgets.isVisible = items.isNotEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
