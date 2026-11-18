package com.example.finsight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finsight.databinding.FragmentAssistantBinding

/**
 * Fragment providing the conversational AI chat interface with suggestion chips and loading state.
 */
class AssistantFragment : Fragment() {

    private var _binding: FragmentAssistantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssistantViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupInput()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = adapter
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun setupChips() {
        binding.chipShopping.setOnClickListener {
            binding.etMessage.setText(binding.chipShopping.text)
            sendMessage()
        }
        binding.chipOverspending.setOnClickListener {
            binding.etMessage.setText(binding.chipOverspending.text)
            sendMessage()
        }
        binding.chipSaving.setOnClickListener {
            binding.etMessage.setText(binding.chipSaving.text)
            sendMessage()
        }
    }

    private fun sendMessage() {
        val query = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (query.isNotEmpty()) {
            viewModel.sendMessage(query)
            binding.etMessage.setText("")
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.rvChatMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.isVisible = isLoading
            binding.btnSend.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
