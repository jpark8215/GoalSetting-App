package com.developerjp.jieungoalsettingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.databinding.FragmentHomeBinding
import com.developerjp.jieungoalsettingapp.ui.dashboard.DashboardViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var buttonGo: Button
    private lateinit var timeBoundButton: TextView
    private lateinit var measurableSeekBar: SeekBar

    private val dashboardViewModel: DashboardViewModel by activityViewModels()
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        dbHelper = DBHelper.getInstance(requireContext()) // Initialize DBHelper using getInstance method

        // Inflate the layout for this fragment using view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the views using view binding
        buttonGo = binding.buttonGo
        timeBoundButton = binding.timeBound
        measurableSeekBar = binding.measurable

        // Set a click listener for the "timeBound" field to show the date picker
        timeBoundButton.setOnClickListener {
            showDatePicker()
        }

        // Set a click listener for the "Go" button
        buttonGo.setOnClickListener {
            try {
                // Retrieve the input values from EditText fields
                val specific = binding.specific.text?.toString()
                val measurable = measurableSeekBar.progress.toString()
                val timeBound = timeBoundButton.text?.toString()

                // Check if all values are not null before creating a new Goal instance
                if (specific?.isNotEmpty() == true && measurable.isNotEmpty() && timeBound?.isNotEmpty() == true && timeBound != getString(R.string.select_date)) {
                    // Insert into specific_table and get the specificId
                    val specificId = dbHelper.insertSpecific(specific)

                    // Get current timestamp
                    val timestamp = System.currentTimeMillis()

                    // Insert into goal_table
                    dbHelper.insertGoalDetail(specificId.toInt(), measurable.toInt(), timeBound, timestamp)

                    // Clear the input fields after clicking the "Go" button
                    binding.specific.text?.clear()
                    measurableSeekBar.progress = 2
                    timeBoundButton.text = getString(R.string.`when`)

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

        return root
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            timeBoundButton.text = dateFormat.format(selectedDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
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
