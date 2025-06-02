package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.data.GoalDetail
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.ParseException
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

    class GoalAdapter(
        private var groupedGoalDetails: Map<Int, List<GoalDetail>>,
        private val viewModel: DashboardViewModel
    ) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val specificId = groupedGoalDetails.keys.elementAt(position)
            val goalDetails = groupedGoalDetails[specificId] ?: listOf()
            holder.bind(goalDetails, viewModel)
        }

        override fun getItemCount(): Int {
            return groupedGoalDetails.size
        }

        fun updateGoalDetails(newGoalDetails: Map<Int, List<GoalDetail>>) {
            groupedGoalDetails = newGoalDetails
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val specificTextView: TextView = itemView.findViewById(R.id.specific_text)
            private val measurableTextView: TextView = itemView.findViewById(R.id.measurable_text)
            private val timeBoundTextView: TextView = itemView.findViewById(R.id.time_bound_text)
            private val barChart: BarChart = itemView.findViewById(R.id.line_chart)
            private val deleteButton: Button = itemView.findViewById(R.id.button_delete)
            private val editButton: Button = itemView.findViewById(R.id.button_edit)
            private val successButton: Button = itemView.findViewById(R.id.button_success)

            private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            private fun parseDate(dateString: String?): Date {
                return try {
                    dateString?.let { dbDateFormat.parse(it) } ?: Date()
                } catch (e: ParseException) {
                    Date()
                }
            }

            fun bind(goalDetails: List<GoalDetail>, viewModel: DashboardViewModel) {
                if (goalDetails.isNotEmpty()) {
                    Log.d("GoalAdapter", "Binding ${goalDetails.size} goal details")

                    // Sort goal details by timestamp to get the latest entry
                    val sortedGoalDetails = goalDetails.sortedByDescending { it.timestamp }
                    val latestGoalDetail = sortedGoalDetails[0]

                    specificTextView.text = latestGoalDetail.specificText
                    measurableTextView.text = "${latestGoalDetail.measurable}%"
                    timeBoundTextView.text =
                        dateFormat.format(parseDate(latestGoalDetail.timeBound))

                    // Get calendar instance for date calculations
                    val calendar = Calendar.getInstance()

                    // Find the earliest date (normalize to start of day)
                    val minTimestamp =
                        sortedGoalDetails.minByOrNull { it.timestamp }?.timestamp?.time ?: 0L
                    calendar.timeInMillis = minTimestamp
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfEarliestDay = calendar.timeInMillis

                    // Create bar entries using normalized date calculations
                    val entries = sortedGoalDetails.map { goalDetail ->
                        // Normalize the goal timestamp to start of day
                        calendar.timeInMillis = goalDetail.timestamp.time
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val goalDayStart = calendar.timeInMillis

                        // Calculate day difference from the earliest day and shift bars to the right
                        val dayOffset =
                            ((goalDayStart - startOfEarliestDay) / (24 * 60 * 60 * 1000)).toFloat() + 0.5f
                        BarEntry(dayOffset, goalDetail.measurable.toFloat())
                    }

                    if (entries.isNotEmpty()) {
                        val dataSet = BarDataSet(entries, "%").apply {
                            color = Color.CYAN
                            valueTextColor = Color.DKGRAY
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return value.toInt().toString()
                                }
                            }
                        }

                        val barData = BarData(dataSet)
                        barData.barWidth = 0.9f
                        barChart.data = barData

                        // Basic chart setup
                        barChart.description.isEnabled = false
                        barChart.legend.isEnabled = false
                        barChart.setDrawGridBackground(false)
                        barChart.setDrawBorders(false)
                        barChart.setScaleEnabled(true)
                        barChart.setPinchZoom(true)
                        barChart.setTouchEnabled(true)
                        barChart.setDragEnabled(true)

                        // Configure X-axis
                        val xAxis = barChart.xAxis
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(true)
                        xAxis.setDrawAxisLine(true)
                        xAxis.setDrawLabels(true)
                        xAxis.textSize = 12f
                        xAxis.textColor = Color.BLACK
                        xAxis.labelRotationAngle = 45f

                        // Calculate the date range using normalized dates
                        val maxDate = parseDate(latestGoalDetail.timeBound).time
                        val daysBetween =
                            ((maxDate - startOfEarliestDay) / (24 * 60 * 60 * 1000)).toInt()
                        val labelCount = minOf(daysBetween + 1, 7) // Show at most 7 labels
                        xAxis.setLabelCount(labelCount, true)

                        // Set X-axis limits with proper padding for shifted bars
                        xAxis.axisMinimum = 0f
                        xAxis.axisMaximum = daysBetween.toFloat() + 1f

                        // Set X-axis formatter with proper date calculation
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return try {
                                    val dayOffset = value.toInt()
                                    val cal = Calendar.getInstance()
                                    cal.timeInMillis = startOfEarliestDay
                                    cal.add(Calendar.DAY_OF_MONTH, dayOffset)

                                    val day = cal.get(Calendar.DAY_OF_MONTH)
                                    val month = cal.get(Calendar.MONTH) + 1
                                    "$month/$day"
                                } catch (e: Exception) {
                                    Log.e("GoalAdapter", "Error formatting date", e)
                                    ""
                                }
                            }
                        }

                        // Configure Y-axis
                        val yAxis = barChart.axisLeft
                        yAxis.setDrawGridLines(true)
                        yAxis.setDrawAxisLine(true)
                        yAxis.setDrawLabels(true)
                        yAxis.textSize = 12f
                        yAxis.textColor = Color.BLACK
                        yAxis.axisMinimum = 0f
                        yAxis.axisMaximum = 100f
                        yAxis.granularity = 10f

                        // Disable right axis
                        barChart.axisRight.isEnabled = false

                        // Calculate visible range
                        val visibleRange = when {
                            daysBetween <= 7 -> daysBetween.toFloat()
                            else -> 7f
                        }

                        // Set visible range and move to latest entry
                        barChart.setVisibleXRange(1f, visibleRange)
                        barChart.moveViewToX(entries.last().x)

                        // Force update
                        barChart.notifyDataSetChanged()
                        barChart.invalidate()
                        Log.d("GoalAdapter", "Chart configuration completed")
                    } else {
                        Log.e("GoalAdapter", "No data available for chart")
                        barChart.clear()
                        barChart.invalidate()
                    }

                    // Hide edit and success buttons if goal is completed
                    if (latestGoalDetail.measurable >= 100) {
                        editButton.visibility = View.GONE
                        successButton.visibility = View.GONE
                        deleteButton.visibility = View.GONE
                    } else {
                        editButton.visibility = View.VISIBLE
                        successButton.visibility = View.VISIBLE
                        deleteButton.visibility = View.VISIBLE
                    }

                    // Delete button functionality with confirmation dialog
                    deleteButton.setOnClickListener {
                        viewModel.showDeleteConfirmation(
                            itemView.context,
                            latestGoalDetail.specificId
                        )
                    }

                    // Edit button functionality
                    editButton.setOnClickListener {
                        viewModel.showEditDialog(itemView.context, latestGoalDetail)
                    }

                    // Success button functionality with congratulatory dialog
                    successButton.setOnClickListener {
                        viewModel.updateGoalDetail(
                            specificId = latestGoalDetail.specificId,
                            specificText = latestGoalDetail.specificText,
                            measurable = 100,
                            timeBound = dateFormat.format(Date(System.currentTimeMillis()))
                        )

                        // Show animated congratulatory dialog
                        val dialogView = LayoutInflater.from(itemView.context)
                            .inflate(R.layout.dialog_congratulations, null)
                        val dialog = AlertDialog.Builder(itemView.context, R.style.RoundedDialog)
                            .setView(dialogView).setCancelable(false).create()

                        dialogView.findViewById<MaterialButton>(R.id.button_ok).setOnClickListener {
                            dialog.dismiss()
                            // Refresh the dashboard to remove the completed goal
                            viewModel.refreshData()
                        }

                        dialog.show()
                    }
                } else {
                    Log.d("GoalAdapter", "No goal details to bind")
                }
            }
        }
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
                        .setBackground(
                            context.resources.getDrawable(R.drawable.rounded_dialog_background, null)
                        )
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .apply {
                            setOnShowListener {
                                // Style the title
                                findViewById<TextView>(android.R.id.title)?.apply {
                                    setTextColor(context.resources.getColor(R.color.colorAccent, null))
                                    textSize = 20f
                                    setPadding(0, 0, 0, 20)
                                }
                                // Style the message
                                findViewById<TextView>(android.R.id.message)?.apply {
                                    setTextColor(context.resources.getColor(R.color.textPrimary, null))
                                    textSize = 17f
                                    setPadding(60, 0, 0, 20)
                                }
                                // Style the dialog
                                getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                                    setTextColor(context.resources.getColor(R.color.purple_500, null))
                                    textSize = 15f
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

    fun updateGoalDetail(
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