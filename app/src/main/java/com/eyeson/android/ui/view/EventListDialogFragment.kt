package com.eyeson.android.ui.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eyeson.android.R
import com.eyeson.android.databinding.DialogEventListBinding
import com.eyeson.android.databinding.ItemEventBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class EventListDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogEventListBinding? = null
    private val viewModel: EventsViewModel by activityViewModels()

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogEventListBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = EventAdapter(viewModel.events)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.newEvent.collect {
                    binding.list.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private inner class ViewHolder(binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val text: TextView = binding.text
    }

    private inner class EventAdapter(private val events: List<Event>) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                ItemEventBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = events[position].eventText
            val color = if (position % 2 == 0) {
                R.color.white
            } else {
                R.color.gray_400
            }

            holder.text.setBackgroundResource(color)
        }

        override fun getItemCount(): Int {
            return events.size
        }
    }

    companion object {
        fun newInstance() = EventListDialogFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}