package com.developerjp.jieungoalsettingapp.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.databinding.FragmentAchievementsBinding

class AchievementsFragment : Fragment() {
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var achievementsViewModel: AchievementsViewModel
    private lateinit var adapter: AchievementsViewModel.CompletedGoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dbHelper = DBHelper.getInstance(requireContext())
        achievementsViewModel = ViewModelProvider(
            this,
            AchievementsViewModel.Companion.Factory(dbHelper)
        )[AchievementsViewModel::class.java]

        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)

        setupViews()
        observeViewModel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        achievementsViewModel.refreshData()
    }

    override fun onResume() {
        super.onResume()
        achievementsViewModel.refreshData()
    }

    private fun setupViews() {
        adapter = AchievementsViewModel.CompletedGoalsAdapter(emptyList(), achievementsViewModel)
        binding.recyclerViewAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AchievementsFragment.adapter
        }
    }

    private fun observeViewModel() {
        achievementsViewModel.completedGoals.observe(viewLifecycleOwner) { goals ->
            adapter.updateGoals(goals)
        }

        achievementsViewModel.totalGoals.observe(viewLifecycleOwner) { count ->
            binding.totalGoals.text = count.toString()
        }

        achievementsViewModel.totalCompletedGoals.observe(viewLifecycleOwner) { count ->
            binding.totalAchievements.text = count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
