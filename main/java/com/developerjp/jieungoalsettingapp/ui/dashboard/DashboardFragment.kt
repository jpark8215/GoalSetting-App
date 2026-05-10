package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.databinding.FragmentDashboardBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var spinnerSelectionSuppressed = false
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
        setupFab()
        setupAd()
        observeViewModel()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.refreshData()
        // Resume ad when fragment resumes
        binding.dashboardAdView.resume()
    }

    override fun onPause() {
        super.onPause()
        // Pause ad when fragment pauses
        binding.dashboardAdView.pause()
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
                if (spinnerSelectionSuppressed) return
                val selectedText = parent?.getItemAtPosition(position).toString()
                dashboardViewModel.filterGoals(selectedText)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun setupFab() {
        val goHome = View.OnClickListener {
            findNavController().navigate(R.id.navigation_home)
        }
        binding.fabAddGoal.setOnClickListener(goHome)
        binding.buttonEmptyCreateGoal.setOnClickListener(goHome)
    }

    private fun setupAd() {
        val adRequest = AdRequest.Builder().build()
        binding.dashboardAdView.loadAd(adRequest)
    }

    private fun observeViewModel() {
        dashboardViewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            val filterAll = getString(R.string.filter_all_goals)
            val specificTexts = goals.map { it.specificText }.distinct().sortedBy { it.lowercase() }
            val spinnerOptions = mutableListOf(filterAll).apply { addAll(specificTexts) }

            spinnerSelectionSuppressed = true
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                spinnerOptions
            ).also { spinnerAdapter ->
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGoals.adapter = spinnerAdapter
                val desired = dashboardViewModel.desiredSpinnerIndex(spinnerOptions)
                val hi = (spinnerOptions.size - 1).coerceAtLeast(0)
                binding.spinnerGoals.setSelection(desired.coerceIn(0, hi))
            }
            binding.spinnerGoals.post {
                spinnerSelectionSuppressed = false
                dashboardViewModel.filterGoals(
                    binding.spinnerGoals.selectedItem?.toString() ?: filterAll
                )
            }
        }

        dashboardViewModel.filteredGoals.observe(viewLifecycleOwner) { filteredGoals ->
            val groupedFilteredGoals = filteredGoals.groupBy { it.specificId }
            adapter.updateGoalDetails(groupedFilteredGoals)
            updateEmptyDashboardState(groupedFilteredGoals.isEmpty())
        }
    }

    private fun updateEmptyDashboardState(showEmptyCard: Boolean) {
        binding.emptyGoalsState.visibility = if (showEmptyCard) View.VISIBLE else View.GONE
        binding.recyclerViewGoals.visibility = if (showEmptyCard) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.dashboardAdView.destroy()
        _binding = null
    }
}