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
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.databinding.FragmentDashboardBinding
import com.developerjp.jieungoalsettingapp.ui.home.Goal

class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by activityViewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

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
        val adapter = GoalListAdapter(emptyList())
        recyclerView.adapter = adapter

        // Observe the allGoals LiveData and update the RecyclerView when it changes
        dashboardViewModel.newGoalList.observe(viewLifecycleOwner, Observer { goals ->
            goals?.let {
                adapter.updateGoals(it)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GoalListAdapter(private var goals: List<Goal>) : RecyclerView.Adapter<GoalListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val specificTextView: TextView = view.findViewById(R.id.specific_text)
        private val measurableTextView: TextView = view.findViewById(R.id.measurable_text)
        private val timeBoundTextView: TextView = view.findViewById(R.id.time_bound_text)

        fun bind(goal: Goal) {
            specificTextView.text = goal.specific
            measurableTextView.text = goal.measurable
            timeBoundTextView.text = goal.timeBound
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<Goal>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
