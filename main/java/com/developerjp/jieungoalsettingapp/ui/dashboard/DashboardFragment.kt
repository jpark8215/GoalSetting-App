package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by activityViewModels()
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

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Set up the RecyclerView
        val recyclerView: RecyclerView = binding.recyclerViewGoals
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter
        adapter = DashboardViewModel.GoalAdapter(emptyList(), dashboardViewModel.dbHelper)
        recyclerView.adapter = adapter

        // Observe the newGoalList LiveData and update the RecyclerView when it changes
        dashboardViewModel.newGoalList.observe(viewLifecycleOwner, Observer { goals ->
            goals?.let {
                adapter.updateGoalDetails(it)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

