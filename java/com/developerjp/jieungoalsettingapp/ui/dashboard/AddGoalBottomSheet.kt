package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.databinding.BottomSheetAddGoalBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.TimeZone

class AddGoalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddGoalBinding.inflate(inflater, container, false)
        dbHelper = DBHelper.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.seekBarValue.text = getString(R.string.progress_percent_format, 0)
        binding.measurable.progress = 0

        binding.measurable.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.seekBarValue.text = getString(R.string.progress_percent_format, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val openDatePicker = View.OnClickListener {
            showMaterialDatePicker()
        }
        binding.timeBound.setOnClickListener(openDatePicker)
        binding.timeBoundCard.setOnClickListener(openDatePicker)

        binding.buttonGo.setOnClickListener {
            saveGoal()
        }
    }

    private fun saveGoal() {
        try {
            hideKeyboard()
            val specific = binding.specific.text?.toString()?.trim().orEmpty()
            val measurable = binding.measurable.progress
            val dateTag = binding.timeBound.tag as? String

            if (specific.isEmpty() || dateTag.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.fill_all_fields,
                    Toast.LENGTH_SHORT
                ).show()
                return
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

                // Notify parent to refresh
                parentFragmentManager.setFragmentResult("add_goal_request", Bundle())

                val navBar = requireActivity().findViewById<View>(R.id.nav_view)
                if (navBar != null) {
                    Snackbar.make(navBar, R.string.snackbar_goal_saved, Snackbar.LENGTH_SHORT)
                        .setAnchorView(navBar)
                        .show()
                } else {
                    Toast.makeText(requireContext(), R.string.snackbar_goal_saved, Toast.LENGTH_SHORT).show()
                }

                dismiss()
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
            binding.timeBound.text = displayFormat
            binding.timeBound.tag = dbFormat
        }
        picker.show(childFragmentManager, "goal_target_date")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "AddGoalBottomSheet"
    }
}
