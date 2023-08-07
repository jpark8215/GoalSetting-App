package com.example.jieungoalsettingapp.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.jieungoalsettingapp.databinding.FragmentHomeBinding
import com.example.jieungoalsettingapp.ui.dashboard.DashboardViewModel
import com.google.android.material.textfield.TextInputEditText
import java.net.URLEncoder

// Goal class representing a user's goal with specific, measurable, attainable, relevant, and time-bound properties
class Goal(
    val specific: String,
    val measurable: String,
    val attainable: String,
    val relevant: String,
    val timeBound: String
) {
    override fun toString(): String {
        // Returns a formatted string representation of the goal's properties
        return "Specific: $specific\nMeasurable: $measurable\nAttainable: $attainable\nRelevant: $relevant\nTime-bound: $timeBound"
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var editTextUserInput: TextInputEditText
    private lateinit var buttonSubmit: Button
    private lateinit var buttonGo: Button

    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the views using view binding
        editTextUserInput = binding.editTextUserInput
        buttonSubmit = binding.buttonSubmit
        buttonGo = binding.buttonGo

        // Set a click listener for the "Submit" button
        buttonSubmit.setOnClickListener {
            val userInput = editTextUserInput.text.toString()
            performGoogleSearch(userInput)
        }

        // Set a click listener for the "Go" button
        buttonGo.setOnClickListener {
            // Retrieve the input values from EditText fields
            val specific = binding.specific.text?.toString()
            val measurable = binding.measurable.text?.toString()
            val attainable = binding.attainable.text?.toString()
            val relevant = binding.relevant.text?.toString()
            val timeBound = binding.timeBound.text?.toString()

            // Check if all values are not null before creating a new Goal instance
            if (specific != null && measurable != null && attainable != null && relevant != null && timeBound != null) {
                // Create a new instance of the Goal class with the retrieved input
                val newGoal = Goal(specific, measurable, attainable, relevant, timeBound)

                // Add the newGoal to the list in DashboardViewModel
                dashboardViewModel.addGoal(newGoal)
            } else {
                showToast("Please fill all fields.")
            }
        }

        return root
    }

    private fun performGoogleSearch(query: String) {
        val encodedQuery = URLEncoder.encode(query, "utf-8")
        val searchUrl = "https://www.google.com/search?q=$encodedQuery"

        // Open the default browser with the search URL
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))

        try {
            startActivity(browserIntent)
        } catch (e: Exception) {
            showToast("Failed to open the browser.")
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        // Show a short toast message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding reference to avoid memory leaks
        _binding = null
    }
}
