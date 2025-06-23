package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.developerjp.jieungoalsettingapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var adapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Initialize ViewModel using the correct constructor
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupRecyclerView()
        setupSpinner()
        observeViewModel()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.refreshData()
    }

    private fun setupRecyclerView() {
        adapter = GoalAdapter(emptyMap(), dashboardViewModel)
        binding.recyclerViewGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DashboardFragment.adapter
        }
    }

    private fun setupSpinner() {
        binding.spinnerGoals.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedText = parent?.getItemAtPosition(position).toString()
                dashboardViewModel.filterGoals(selectedText)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle case when nothing is selected
            }
        }
    }

    private fun observeViewModel() {
        // Observe all goals for spinner population
        dashboardViewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            val specificTexts = goals.map { it.specificText }.distinct()
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                specificTexts
            ).also { spinnerAdapter ->
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGoals.adapter = spinnerAdapter
            }
        }

        // Observe goal list for RecyclerView updates
        dashboardViewModel.goalList.observe(viewLifecycleOwner) { groupedGoals ->
            adapter.updateGoalDetails(groupedGoals)
        }

        // Observe filtered goals
        dashboardViewModel.filteredGoals.observe(viewLifecycleOwner) { filteredGoals ->
            val groupedFilteredGoals = filteredGoals.groupBy { it.specificId }
            adapter.updateGoalDetails(groupedFilteredGoals)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}