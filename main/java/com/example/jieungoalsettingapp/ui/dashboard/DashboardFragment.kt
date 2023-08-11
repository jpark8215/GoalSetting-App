package com.example.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jieungoalsettingapp.R
import com.example.jieungoalsettingapp.databinding.FragmentDashboardBinding
import com.example.jieungoalsettingapp.ui.home.Goal


// The GoalListAdapter is responsible for displaying Goal items in the RecyclerView
class GoalListAdapter(private var goals: List<Goal>) : RecyclerView.Adapter<GoalListAdapter.ViewHolder>() {
    // The ViewHolder holds references to the views in the item layout
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val specificTextView: TextView = view.findViewById(R.id.specific)
        private val measurableTextView: TextView = view.findViewById(R.id.measurable)
        private val attainableTextView: TextView = view.findViewById(R.id.attainable)
        private val relevantTextView: TextView = view.findViewById(R.id.relevant)
        private val timeBoundTextView: TextView = view.findViewById(R.id.timeBound)

        // Bind the goal properties to the views in the item layout
        fun bind(goal: Goal) {
            specificTextView.text = goal.specific
            measurableTextView.text = goal.measurable
            attainableTextView.text = goal.attainable
            relevantTextView.text = goal.relevant
            timeBoundTextView.text = goal.timeBound
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item layout and create a ViewHolder instance
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the data at the given position to the ViewHolder
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int {
        // Return the number of items in the list
        return goals.size
    }

    fun updateGoals(newGoals: List<Goal>) {
        // Update the list of goals and notify the adapter about the changes
        goals = newGoals
    }
}

class DashboardFragment : Fragment() {
    // Get an instance of the DashboardViewModel using viewModels() delegate
    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Create a list to store newGoal objects
    private val newGoalList = mutableListOf<Goal>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
//        val root: View = binding.root
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the RecyclerView
        val recyclerView: RecyclerView = binding.recyclerViewGoals
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GoalListAdapter(emptyList()) // Start with an empty list of goals
        recyclerView.adapter = adapter

        // Observe the newGoalList LiveData and update the RecyclerView when it changes
        dashboardViewModel.newGoalList.observe(viewLifecycleOwner, Observer { goals ->
            displayGoalList(goals)
        })
    }

    private fun displayGoalList(goals: List<Goal>) {
        // Update the RecyclerView adapter with the new list of goals
        val adapter = binding.recyclerViewGoals.adapter as GoalListAdapter
        adapter.updateGoals(goals)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the view binding instance
        _binding = null
    }
}
