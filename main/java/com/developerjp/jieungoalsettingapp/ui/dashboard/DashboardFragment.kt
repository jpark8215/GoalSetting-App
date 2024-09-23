package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.developerjp.jieungoalsettingapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardViewModel.GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DashboardViewModel.GoalAdapter(emptyMap(), dashboardViewModel)
        binding.recyclerViewGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGoals.adapter = adapter

        dashboardViewModel.goalList.observe(viewLifecycleOwner) { groupedGoalDetails ->
            adapter.updateGoalDetails(groupedGoalDetails)
        }
    }


    override fun onResume() {
        super.onResume()
        dashboardViewModel
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
