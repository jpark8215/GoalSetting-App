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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener

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
                val selectedText = parent?.getItemAtPosition(position).toString()
                dashboardViewModel.filterGoals(selectedText)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle case when nothing is selected
            }
        }
    }

    private fun setupAd() {
        val adRequest = AdRequest.Builder().build()
        binding.dashboardAdView.loadAd(adRequest)
        
        binding.dashboardAdView.adListener = object : AdListener() {

        }
    }

    private fun observeViewModel() {
        // Observe all goals for spinner population
        dashboardViewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            val specificTexts = goals.map { it.specificText }.distinct()
            
            // Store current selection before updating adapter
            val currentSelection = binding.spinnerGoals.selectedItem?.toString()
            
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                specificTexts
            ).also { spinnerAdapter ->
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGoals.adapter = spinnerAdapter
                
                // Restore selection if it still exists in the new list
                currentSelection?.let { selection ->
                    val newPosition = specificTexts.indexOf(selection)
                    if (newPosition >= 0) {
                        binding.spinnerGoals.setSelection(newPosition)
                    }
                }
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
        binding.dashboardAdView.destroy()
        _binding = null
    }
}