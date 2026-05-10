package com.developerjp.jieungoalsettingapp.ui.achievements

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
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

        val latestEntries = goalsBySpecificId.mapValues { (_, goals) ->
            goals.maxByOrNull { it.timestamp }
        }

        val completedGoalsList = latestEntries.values
            .filterNotNull()
            .filter { it.measurable == 100 }

        _completedGoals.value = completedGoalsList
        _totalGoals.value = goalsBySpecificId.size
        _totalCompletedGoals.value = completedGoalsList.size
    }

    fun showEditDialog(context: Context, goalDetail: GoalDetail) {
        val latestGoalDetail = getLatestGoalDetail(goalDetail.specificId) ?: goalDetail

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_goal, null)
        val editSpecificText = dialogView.findViewById<EditText>(R.id.edit_specific_text)
        val editMeasurableSeekbar = dialogView.findViewById<SeekBar>(R.id.edit_measurable_seekbar)
        val editMeasurableValue = dialogView.findViewById<TextView>(R.id.edit_measurable_value)
        val editTimeBoundDatePicker =
            dialogView.findViewById<DatePicker>(R.id.edit_time_bound_datepicker)
        val buttonSave = dialogView.findViewById<Button>(R.id.button_save)
        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)

        editSpecificText.setText(latestGoalDetail.specificText)
        editMeasurableSeekbar.progress = latestGoalDetail.measurable
        editMeasurableValue.text = context.resources.getString(
            R.string.progress_percent_format,
            latestGoalDetail.measurable
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

        val cal = Calendar.getInstance()
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        try {
            val date = dbFormat.parse(latestGoalDetail.timeBound)
            if (date != null) {
                cal.time = date
            } else {
                val displayDate = displayFormat.parse(latestGoalDetail.timeBound)
                cal.time = displayDate ?: Date()
            }
        } catch (_: Exception) {
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

            if (specificText != latestGoalDetail.specificText
                && dbHelper.isSpecificExists(specificText)
            ) {
                MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
                    .setTitle(R.string.duplicate_goal_title)
                    .setMessage(R.string.duplicate_goal_message)
                    .setPositiveButton(R.string.action_ok) { d, _ -> d.dismiss() }
                    .show()
                return@setOnClickListener
            }

            updateGoalDetailAfterEdit(latestGoalDetail.specificId, specificText, measurable, timeBound)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateGoalDetailAfterEdit(
        specificId: Int,
        specificText: String,
        measurable: Int,
        timeBound: String
    ) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        refreshData()
    }

    fun confirmDeleteAchievement(context: Context, specificId: Int) {
        MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_RoundedDestructive)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteGoalsBySpecificId(specificId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    fun getLatestGoalDetail(specificId: Int): GoalDetail? =
        dbHelper.getLatestGoalDetailBySpecificId(specificId)

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

        inner class ViewHolder(private val parent: android.view.View) :
            RecyclerView.ViewHolder(parent) {
            private val specificTextView: TextView = parent.findViewById(R.id.specific_text)
            private val targetLine: TextView = parent.findViewById(R.id.target_line)
            private val completedLine: TextView = parent.findViewById(R.id.completed_line)
            private val menuButton: ImageButton = parent.findViewById(R.id.button_goal_menu)

            private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            private fun parseDate(dateString: String?): Date {
                if (dateString.isNullOrEmpty()) {
                    return Date()
                }

                return try {
                    dbDateFormat.parse(dateString)!!
                } catch (e: ParseException) {
                    try {
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(dateString)
                            ?: Date()
                    } catch (e2: ParseException) {
                        Date()
                    }
                }
            }

            fun bind(goal: GoalDetail) {
                val ctx = parent.context
                specificTextView.text = goal.specificText
                targetLine.text =
                    ctx.getString(R.string.achievement_row_target, dateFormat.format(parseDate(goal.timeBound)))
                completedLine.text =
                    ctx.getString(R.string.achievement_row_completed, dateFormat.format(goal.timestamp))

                menuButton.setOnClickListener {
                    val popup = PopupMenu(ctx, menuButton)
                    popup.menuInflater.inflate(R.menu.popup_goal_achievement, popup.menu)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.menu_edit_goal -> viewModel.showEditDialog(ctx, goal)
                            R.id.menu_delete_goal -> viewModel.confirmDeleteAchievement(ctx, goal.specificId)
                        }
                        true
                    }
                    popup.show()
                }

                parent.setOnLongClickListener {
                    menuButton.performClick()
                    true
                }
            }
        }

        override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parentView.context)
                .inflate(R.layout.item_achievement, parentView, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(completedGoals[position])
        }

        override fun getItemCount(): Int = completedGoals.size

        fun updateGoals(newGoals: List<GoalDetail>) {
            completedGoals = newGoals
            notifyDataSetChanged()
        }
    }
}
