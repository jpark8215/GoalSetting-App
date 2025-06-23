package com.developerjp.jieungoalsettingapp.ui.achievements

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.data.GoalDetail
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AchievementsViewModel(private val dbHelper: DBHelper) : ViewModel() {
    private val _completedGoals = MutableLiveData<List<GoalDetail>>()
    private val _totalGoals = MutableLiveData<Int>()
    private val _totalCompletedGoals = MutableLiveData<Int>()

    val completedGoals: LiveData<List<GoalDetail>> = _completedGoals
    val totalGoals: LiveData<Int> = _totalGoals
    val totalCompletedGoals: LiveData<Int> = _totalCompletedGoals

    init {
        refreshData()
    }

    fun refreshData() {
        fetchGoals()
    }

    private fun fetchGoals() {
        val allGoals = dbHelper.allGoalDetailsWithSpecificText
        val goalsBySpecificId = allGoals.groupBy { it.specificId }

        // Get the latest entry for each specific ID
        val latestEntries = goalsBySpecificId.mapValues { (_, goals) ->
            goals.maxByOrNull { it.timestamp }
        }

        // Filter goals where the latest entry is 100% and remove nulls
        val completedGoals = latestEntries.values
            .filterNotNull()
            .filter { it.measurable == 100 }

        _completedGoals.value = completedGoals
        _totalGoals.value = goalsBySpecificId.size
        _totalCompletedGoals.value = completedGoals.size
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
        val displayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            // First try parsing with display format (MM/dd/yyyy)
            val date = displayFormat.parse(goalDetail.timeBound)
            if (date != null) {
                cal.time = date
            } else {
                // If that fails, try parsing with database format (yyyy-MM-dd)
                val dbDate = dbFormat.parse(goalDetail.timeBound)
                cal.time = dbDate ?: Date()
            }
        } catch (e: Exception) {
            // If both parsing attempts fail, use current date
            cal.time = Date()
        }

        editTimeBoundDatePicker.updateDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
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
                                getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
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

    private fun updateGoalDetail(
        specificId: Int,
        specificText: String,
        measurable: Int,
        timeBound: String
    ) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        refreshData()
    }

    fun deleteGoalsBySpecificId(specificId: Int) {
        dbHelper.deleteGoalsBySpecificId(specificId)
        refreshData()
    }

    companion object {
        class Factory(private val dbHelper: DBHelper) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
                    return AchievementsViewModel(dbHelper) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    class CompletedGoalsAdapter(
        private var completedGoals: List<GoalDetail>,
        private val viewModel: AchievementsViewModel
    ) : RecyclerView.Adapter<CompletedGoalsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val specificTextView: TextView = itemView.findViewById(R.id.specific_text)
            private val timeBoundTextView: TextView = itemView.findViewById(R.id.time_bound_text)

            private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            private fun parseDate(dateString: String?): Date {
                return try {
                    dateString?.let { dbDateFormat.parse(it) } ?: Date()
                } catch (e: ParseException) {
                    Date()
                }
            }

            init {
                itemView.setOnLongClickListener { view ->
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val goal = completedGoals[position]
                        // Show options dialog
                        val options = arrayOf("Edit", "Delete")
                        MaterialAlertDialogBuilder(
                            view.context,
                            R.style.MaterialAlertDialog_Rounded
                        )
                            .setTitle("Goal Options")
                            .setItems(options) { _, which ->
                                when (which) {
                                    0 -> viewModel.showEditDialog(view.context, goal)
                                    1 -> {
                                        // Show delete confirmation
                                        MaterialAlertDialogBuilder(
                                            view.context,
                                            R.style.MaterialAlertDialog_Rounded
                                        )
                                            .setTitle("Confirm Delete")
                                            .setMessage("Are you sure you want to delete this goal?")
                                            .setPositiveButton("Delete") { _, _ ->

                                                viewModel.deleteGoalsBySpecificId(goal.specificId)
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                }
                            }
                            .show()
                        true
                    } else {
                        false
                    }
                }
            }

            fun bind(goal: GoalDetail) {
                specificTextView.text = goal.specificText
                timeBoundTextView.text = dateFormat.format(parseDate(goal.timeBound))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val goal = completedGoals[position]
            holder.bind(goal)
        }

        override fun getItemCount(): Int = completedGoals.size

        fun updateGoals(newGoals: List<GoalDetail>) {
            completedGoals = newGoals
            notifyDataSetChanged()
        }
    }
}
