package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.data.GoalDetail
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    private val dbHelper = DBHelper.getInstance(application)

    private val _goalList = MutableLiveData<Map<Int, List<GoalDetail>>>()
    val goalList: LiveData<Map<Int, List<GoalDetail>>> get() = _goalList

    private val _allGoals = MutableLiveData<List<GoalDetail>>()
    val allGoals: LiveData<List<GoalDetail>> = _allGoals

    private val _filteredGoals = MutableLiveData<List<GoalDetail>>()
    val filteredGoals: LiveData<List<GoalDetail>> = _filteredGoals

    init {
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        fetchGoals()
    }

    fun refreshData() {
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        fetchGoals()
    }

    // Fetch all goal details from the database
    private fun fetchGoalsFromDatabase(): List<GoalDetail> {
        // Get all goals and group them by specificId
        val allGoals = dbHelper.allGoalDetailsWithSpecificText
        val goalsBySpecificId = allGoals.groupBy { it.specificId }

        // Filter out goals where the latest entry is 100%
        return allGoals.filter { detail ->
            val latestEntry = goalsBySpecificId[detail.specificId]?.maxByOrNull { it.timestamp }
            (latestEntry?.measurable ?: 0) < 100
        }.map { detail ->
            GoalDetail(
                detail.id,
                detail.specificId,
                detail.measurable,
                detail.timeBound,
                detail.timestamp,
                detail.specificText
            )
        }
    }

    // Delete goals by specific ID
    private fun deleteGoalsBySpecificId(specificId: Int) {
        dbHelper.deleteGoalsBySpecificId(specificId)
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        refreshData()
    }

    private fun fetchGoals() {
        val allGoals = dbHelper.allGoalDetailsWithSpecificText
        val goalsBySpecificId = allGoals.groupBy { it.specificId }

        // Filter out goals where the latest entry is 100%
        _allGoals.value = allGoals.filter { detail ->
            val latestEntry = goalsBySpecificId[detail.specificId]?.maxByOrNull { it.timestamp }
            (latestEntry?.measurable ?: 0) < 100
        }
        _filteredGoals.value = _allGoals.value
    }

    fun filterGoals(selectedText: String) {
        _filteredGoals.value = _allGoals.value?.filter { it.specificText == selectedText }
    }

    fun updateGoalProgress(specificId: Int, progress: Int) {
        dbHelper.updateGoalProgress(specificId, progress)
        // We don't need to refresh the whole list here, but we'll need to update the item.
        // For now, a full refresh is acceptable to ensure UI consistency.
        refreshData()
    }

    fun showCongratulationsDialog(context: Context, onDismiss: () -> Unit) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_congratulations, null)
        val dialog = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
            .setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<MaterialButton>(R.id.button_ok).setOnClickListener {
            dialog.dismiss()
            onDismiss()
        }

        dialog.show()
    }

    fun showEditDialog(context: Context, goalDetail: GoalDetail) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_goal, null)
        val editSpecificText = dialogView.findViewById<EditText>(R.id.edit_specific_text)
        val editMeasurableSeekbar = dialogView.findViewById<SeekBar>(R.id.edit_measurable_seekbar)
        val editMeasurableValue = dialogView.findViewById<TextView>(R.id.edit_measurable_value)
        val editTimeBoundDatePicker =
            dialogView.findViewById<DatePicker>(R.id.edit_time_bound_datepicker)
        val buttonSave = dialogView.findViewById<Button>(R.id.button_save)
        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)

        editSpecificText.setText(goalDetail.specificText)
        editMeasurableSeekbar.progress = goalDetail.measurable
        editMeasurableValue.text = "${goalDetail.measurable}%"

        editMeasurableSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                editMeasurableValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set DatePicker to current goal's time bound
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(goalDetail.timeBound)
        cal.time = date ?: Date()

        editTimeBoundDatePicker.updateDate(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )

        val dialog = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val specificText = editSpecificText.text.toString()
            val measurable = editMeasurableSeekbar.progress
            val year = editTimeBoundDatePicker.year
            val month = editTimeBoundDatePicker.month
            val day = editTimeBoundDatePicker.dayOfMonth
            val timeBound = "$year-${month + 1}-$day"

            // Check if the new title is different from the current one
            if (specificText != goalDetail.specificText) {
                // Check for duplicates only if the title has changed
                if (dbHelper.isSpecificExists(specificText)) {
                    // Show error dialog if duplicate exists
                    MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
                        .setTitle("Duplicate Goal")
                        .setMessage("This goal title has already been claimed\nby a legendary quest.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .apply {
                            setOnShowListener {
                                // Style the title
                                findViewById<TextView>(android.R.id.title)?.apply {
                                    setTextColor(resources.getColor(R.color.colorAccent, null))
                                    textSize = 14f
                                    setPadding(0, 0, 0, 20)
                                }
                                // Style the message
                                findViewById<TextView>(android.R.id.message)?.apply {
                                    setTextColor(resources.getColor(R.color.textPrimary, null))
                                    textSize = 14f
                                    setPadding(70, 0, 0, 20)
                                }
                                // Style the dialog
                                getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                                    setTextColor(resources.getColor(R.color.purple_500, null))
                                    textSize = 12f
                                }
                            }
                        }
                        .show()
                    return@setOnClickListener
                }
            }

            updateGoalDetail(goalDetail.specificId, specificText, measurable, timeBound)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showDeleteConfirmation(context: Context, specificId: Int) {
        MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this goal?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGoalsBySpecificId(specificId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGoalDetail(
        specificId: Int,
        specificText: String,
        measurable: Int,
        timeBound: String
    ) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        refreshData()
    }
}