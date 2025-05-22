package com.developerjp.jieungoalsettingapp.ui.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var buttonGo: Button
    private lateinit var timeBoundButton: TextView
    private lateinit var measurableSeekBar: SeekBar
    private lateinit var seekBarValue: TextView  // TextView to display the SeekBar value

    private val dashboardViewModel: DashboardViewModel by activityViewModels()
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        dbHelper =
            DBHelper.getInstance(requireContext()) // Initialize DBHelper using getInstance method

        // Inflate the layout for this fragment using view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the views using view binding
        buttonGo = binding.buttonGo
        timeBoundButton = binding.timeBound
        measurableSeekBar = binding.measurable
        seekBarValue = binding.seekBarValue  // Initialize the seekBarValue TextView

        // Set the initial SeekBar value to the TextView
        seekBarValue.text = measurableSeekBar.progress.toString()

        // Set a listener to update the TextView as the SeekBar moves
        measurableSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Optional: Do something when tracking starts
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Optional: Do something when tracking stops
            }
        })

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
                val timeBound = timeBoundButton.text.toString()

                // Check if all values are not null before creating a new Goal instance
                if (!specific.isNullOrEmpty() && measurable.isNotEmpty() && timeBound != getString(R.string.`when`)
                ) {
                    if (dbHelper.isSpecificExists(specific)) {
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.MaterialAlertDialog_Rounded
                        )
                            .setTitle("Duplicate Goal")
                            .setMessage("Uh oh! This goal title has already been claimed by a legendary quest.")
                            .setBackground(
                                resources.getDrawable(
                                    R.drawable.rounded_dialog_background,
                                    null
                                )
                            )
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                            .apply {
                                setOnShowListener {
                                    // Style the title
                                    findViewById<TextView>(android.R.id.title)?.apply {
                                        setTextColor(resources.getColor(R.color.colorAccent, null))
                                        textSize = 20f
                                        setPadding(0, 0, 0, 20)
                                    }
                                    // Style the message
                                    findViewById<TextView>(android.R.id.message)?.apply {
                                        setTextColor(resources.getColor(R.color.textPrimary, null))
                                        textSize = 17f
                                        setPadding(50, 0, 0, 20)
                                    }
                                    // Style the dialog
                                    getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                                        setTextColor(resources.getColor(R.color.purple_500, null))
                                        textSize = 15f
                                    }
                                }
                            }
                            .show()

                    } else {

                        // Insert into specific_table and get the specificId
                        val specificId = dbHelper.insertSpecific(specific)

                        // Get current timestamp
                        val timestamp = System.currentTimeMillis()

                        // Insert into goal_table
                        dbHelper.insertGoalDetail(
                            specificId.toInt(),
                            measurable.toInt(),
                            timeBound,
                            timestamp
                        )

                        // Clear the input fields after clicking the "Go" button
                        binding.specific.text?.clear()
                        measurableSeekBar.progress = 20
                        timeBoundButton.text = getString(R.string.`when`)

                        // Use Navigation component to navigate to the dashboard view
                        findNavController().popBackStack()
                        findNavController().navigate(R.id.navigation_dashboard)
                    }
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
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                // Format for display
                val displayFormat =
                    String.format("%02d/%02d/%d", selectedMonth + 1, selectedDay, selectedYear)
                // Format for database storage
                val dbFormat =
                    String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                timeBoundButton.text = displayFormat
                // Store the database format in a tag for later use
                timeBoundButton.tag = dbFormat
            },
            year, month, day
        )

        datePickerDialog.show()
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
