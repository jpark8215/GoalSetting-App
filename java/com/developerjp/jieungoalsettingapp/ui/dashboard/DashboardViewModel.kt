package com.developerjp.jieungoalsettingapp.ui.dashboard

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

    private val dbHelper = DBHelper.getInstance(application)

    private val _goalList = MutableLiveData<Map<Int, List<GoalDetail>>>()
    val goalList: LiveData<Map<Int, List<GoalDetail>>> get() = _goalList

    private val _allGoals = MutableLiveData<List<GoalDetail>>()
    val allGoals: LiveData<List<GoalDetail>> = _allGoals

    private val _filteredGoals = MutableLiveData<List<GoalDetail>>()
    val filteredGoals: LiveData<List<GoalDetail>> = _filteredGoals

    /** Spinner option "show all"; null selection means filter shows every active goal. */
    private var currentSelectedGoalText: String? = null

    private val allGoalsFilterLabel: String
        get() = getApplication<Application>().getString(R.string.filter_all_goals)

    init {
        refreshFromDbPreservingSpinnerSelection()
    }

    fun refreshData() {
        refreshFromDbPreservingSpinnerSelection()
    }

    /** Index to select in spinner options `[allGoalsLabel, ...titles]`. */
    fun desiredSpinnerIndex(spinnerLabels: List<String>): Int {
        val selected = currentSelectedGoalText ?: return 0
        val idx = spinnerLabels.indexOf(selected)
        return if (idx >= 0) idx else 0
    }

    fun refreshDataPreservingSelection(selectedGoalText: String? = null) {
        val preserved = selectedGoalText ?: currentSelectedGoalText
        refreshFromDbPreservingSpinnerSelection(preserved)
    }

    private fun refreshFromDbPreservingSpinnerSelection(preserveGoalTitle: String? = null) {
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }

        val allGoals = dbHelper.allGoalDetailsWithSpecificText
        val goalsBySpecificId = allGoals.groupBy { it.specificId }
        val activeIncomplete = allGoals.filter { detail ->
            val latestEntry = goalsBySpecificId[detail.specificId]?.maxByOrNull { it.timestamp }
            (latestEntry?.measurable ?: 0) < 100
        }
        _allGoals.value = activeIncomplete

        val textToRestore = preserveGoalTitle ?: currentSelectedGoalText
        currentSelectedGoalText = when {
            textToRestore == null -> null
            activeIncomplete.none { it.specificText == textToRestore } -> null
            else -> textToRestore
        }
        applyGoalFilter()
    }

    private fun applyGoalFilter() {
        val all = _allGoals.value.orEmpty()
        val title = currentSelectedGoalText
        _filteredGoals.value =
            if (title == null) all else all.filter { it.specificText == title }
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
        refreshDataPreservingSelection()
    }

    fun filterGoals(selectedSpinnerText: String) {
        currentSelectedGoalText =
            if (selectedSpinnerText == allGoalsFilterLabel) null else selectedSpinnerText
        applyGoalFilter()
    }

    fun updateGoalProgress(specificId: Int, progress: Int) {
        dbHelper.updateGoalProgress(specificId, progress)
        // Refresh data while preserving the current selection
        refreshDataPreservingSelection()
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
        editMeasurableValue.text = context.resources.getString(
            R.string.progress_percent_format,
            goalDetail.measurable
        )

        editMeasurableSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                editMeasurableValue.text = context.resources.getString(
                    R.string.progress_percent_format,
                    progress
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set DatePicker to current goal's time bound
        val cal = Calendar.getInstance()
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        try {
            // First try parsing with database format (yyyy-MM-dd)
            val date = dbFormat.parse(goalDetail.timeBound)
            if (date != null) {
                cal.time = date
            } else {
                // If that fails, try parsing with display format (MM/dd/yyyy)
                val displayDate = displayFormat.parse(goalDetail.timeBound)
                cal.time = displayDate ?: Date()
            }
        } catch (e: Exception) {
            // If both parsing attempts fail, use current date
            cal.time = Date()
        }

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

            if (specificText != goalDetail.specificText
                && dbHelper.isSpecificExists(specificText)
            ) {
                MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
                    .setTitle(R.string.duplicate_goal_title)
                    .setMessage(R.string.duplicate_goal_message)
                    .setPositiveButton(R.string.action_ok) { d, _ -> d.dismiss() }
                    .show()
                return@setOnClickListener
            }

            updateGoalDetail(
                goalDetail.specificId,
                goalDetail.specificText,
                specificText,
                measurable,
                timeBound
            )
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showDeleteConfirmation(context: Context, specificId: Int) {
        MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_RoundedDestructive)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteGoalsBySpecificId(specificId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun updateGoalDetail(
        specificId: Int,
        oldSpecificText: String,
        specificText: String,
        measurable: Int,
        timeBound: String
    ) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        val spinnerTitleToPreserve =
            if (specificText != oldSpecificText) specificText else currentSelectedGoalText
        refreshDataPreservingSelection(spinnerTitleToPreserve)
    }
}