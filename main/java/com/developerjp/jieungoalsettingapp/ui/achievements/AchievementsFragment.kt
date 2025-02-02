package com.developerjp.jieungoalsettingapp.ui.achievements

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.databinding.FragmentAchievementsBinding
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
        // When creating ViewModel
        achievementsViewModel = ViewModelProvider(
            this,
            AchievementsViewModel.Companion.Factory(dbHelper)
        )[AchievementsViewModel::class.java]

        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)

        setupViews()
        observeViewModel()

        return binding.root
    }

    private fun setupViews() {
        // Initialize RecyclerView
        adapter = AchievementsViewModel.CompletedGoalsAdapter(emptyList(), viewModel = achievementsViewModel)
        binding.recyclerViewAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AchievementsFragment.adapter
        }

        // Setup submit button
        binding.buttonSubmit.setOnClickListener {
            binding.editTextUserInput.text?.toString()?.let { input ->
                if (input.isNotEmpty()) {
                    performGoogleSearch(input)
                } else {
                    showToast("Please enter a search term")
                }
            }
        }
    }

    private fun observeViewModel() {
        achievementsViewModel.completedGoals.observe(viewLifecycleOwner) { completedGoals ->
            adapter.updateGoals(completedGoals)
        }
    }

    private fun performGoogleSearch(query: String) {
        try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val searchUrl = "https://www.google.com/search?q=$encodedQuery"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            startActivity(browserIntent)
        } catch (e: Exception) {
            showToast("Failed to open the browser")
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}