package com.developerjp.jieungoalsettingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.databinding.FragmentHomeBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.TimeZone

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var buttonGo: Button
    private lateinit var timeBoundButton: TextView
    private lateinit var measurableSeekBar: SeekBar
    private lateinit var seekBarValue: TextView

    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dbHelper = DBHelper.getInstance(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        buttonGo = binding.buttonGo
        timeBoundButton = binding.timeBound
        measurableSeekBar = binding.measurable
        seekBarValue = binding.seekBarValue

        seekBarValue.text =
            getString(R.string.progress_percent_format, measurableSeekBar.progress)

        measurableSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarValue.text = getString(R.string.progress_percent_format, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        timeBoundButton.setOnClickListener {
            showMaterialDatePicker()
        }

        buttonGo.setOnClickListener {
            try {
                hideKeyboard()
                val specific = binding.specific.text?.toString()?.trim().orEmpty()
                val measurable = measurableSeekBar.progress
                val dateTag = timeBoundButton.tag as? String

                if (specific.isEmpty() || dateTag.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.fill_all_fields,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (dbHelper.isSpecificExists(specific)) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.MaterialAlertDialog_Rounded
                    )
                        .setTitle(R.string.duplicate_goal_title)
                        .setMessage(R.string.duplicate_goal_message)
                        .setPositiveButton(R.string.action_ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    val specificId = dbHelper.insertSpecific(specific)
                    val timestamp = System.currentTimeMillis()
                    dbHelper.insertGoalDetail(
                        specificId.toInt(),
                        measurable,
                        dateTag,
                        timestamp
                    )

                    binding.specific.text?.clear()
                    measurableSeekBar.progress = 20
                    seekBarValue.text =
                        getString(R.string.progress_percent_format, measurableSeekBar.progress)
                    timeBoundButton.text = getString(R.string.`when`)
                    timeBoundButton.tag = null

                    val navBar = requireActivity().findViewById<View>(R.id.nav_view)
                    Snackbar.make(binding.root, R.string.snackbar_goal_saved, Snackbar.LENGTH_SHORT)
                        .setAnchorView(navBar)
                        .show()

                    findNavController().popBackStack(R.id.navigation_dashboard, false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_generic_format, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }

        return root
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showMaterialDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_target_date))
            .build()
        picker.addOnPositiveButtonClickListener { selectionMillis: Long ->
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectionMillis
            }
            val y = utc.get(Calendar.YEAR)
            val mo = utc.get(Calendar.MONTH) + 1
            val day = utc.get(Calendar.DAY_OF_MONTH)
            val displayFormat = String.format("%02d/%02d/%d", mo, day, y)
            val dbFormat = String.format("%d-%02d-%02d", y, mo, day)
            timeBoundButton.text = displayFormat
            timeBoundButton.tag = dbFormat
        }
        picker.show(childFragmentManager, "goal_target_date")
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
