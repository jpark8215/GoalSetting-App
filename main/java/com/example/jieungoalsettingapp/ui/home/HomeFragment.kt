package com.example.jieungoalsettingapp.ui.home

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.jieungoalsettingapp.R
import com.example.jieungoalsettingapp.databinding.FragmentHomeBinding
import com.example.jieungoalsettingapp.ui.dashboard.DashboardViewModel

// Goal class representing a user's goal with specific, measurable, and time-bound properties
class Goal(
    val specific: String,
    val measurable: String,
    val timeBound: String
) {
    override fun toString(): String {
        // Returns a formatted string representation of the goal's properties
        return "Specific: $specific\nMeasurable: $measurable\nTime-bound: $timeBound"
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var buttonGo: Button

    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        // Inflate the layout for this fragment using view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the views using view binding
        buttonGo = binding.buttonGo

        // Set a click listener for the "Go" button
        buttonGo.setOnClickListener {
            try {
                // Retrieve the input values from EditText fields
                val specific = binding.specific.text?.toString()
                val measurable = binding.measurable.text?.toString()
                val timeBound = binding.timeBound.text?.toString()

                // Check if all values are not null before creating a new Goal instance
                if (specific?.isNotEmpty() == true && measurable?.isNotEmpty() == true && timeBound?.isNotEmpty() == true) {
                    // Create a new instance of the Goal class with the retrieved input
                    val newGoal = Goal(specific, measurable, timeBound)

                    // Add the newGoal to the list in DashboardViewModel
                    dashboardViewModel.addGoal(newGoal)

                    // Clear the input fields after clicking the "Go" button
                    binding.specific.text?.clear()
                    binding.measurable.text?.clear()
                    binding.timeBound.text?.clear()

                    // Use Navigation component to navigate to the dashboard view
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.navigation_dashboard)

                } else {
                    showToast("Please fill all fields.")
                }
            } catch (e: Exception) {
                showToast("An error occurred: ${e.message}")
                e.printStackTrace()
            }
        }

//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
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
