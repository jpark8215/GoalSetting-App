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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
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

    // Add a new goal detail to the database
    fun addGoal(goalDetail: GoalDetail) {
        val specificId = dbHelper.insertSpecific(goalDetail.specificText)
        val timestamp = System.currentTimeMillis()

        dbHelper.insertGoalDetail(
            specificId.toInt(), goalDetail.measurable, goalDetail.timeBound, timestamp
        )
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        refreshData()
    }

    // Delete goals by specific ID
    fun deleteGoalsBySpecificId(specificId: Int) {
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
            private val lineChart: LineChart = itemView.findViewById(R.id.line_chart)
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

                    // Create chart entries using all goal details
                    val entries = sortedGoalDetails.map { goalDetail ->
                        val timestampFloat = goalDetail.timestamp.time.toFloat()
                        Log.d(
                            "GoalAdapter",
                            "Timestamp: ${goalDetail.timestamp}, Float: $timestampFloat, Measurable: ${goalDetail.measurable}"
                        )
                        Entry(timestampFloat, goalDetail.measurable.toFloat())
                    }

                    if (entries.isNotEmpty()) {
                        val dataSet = LineDataSet(entries, "%").apply {
                            color = Color.BLUE
                            valueTextColor = Color.BLACK
                            lineWidth = 2f
                            setDrawCircles(true)
                            setDrawCircleHole(true)
                            setDrawValues(true)
                            mode = LineDataSet.Mode.LINEAR
                            setDrawFilled(false)
                            circleRadius = 6f
                            circleHoleRadius = 3f
                        }

                        val lineData = LineData(dataSet)
                        lineChart.data = lineData

                        // Basic chart setup
                        lineChart.description.isEnabled = false
                        lineChart.legend.isEnabled = false
                        lineChart.setDrawGridBackground(false)
                        lineChart.setDrawBorders(false)
                        lineChart.setScaleEnabled(true)
                        lineChart.setPinchZoom(true)
                        lineChart.setTouchEnabled(true)
                        lineChart.setDragEnabled(true)

                        // Calculate date range
                        val minTimestamp = sortedGoalDetails.minByOrNull { it.timestamp }?.timestamp?.time ?: 0L
                        val maxDate = parseDate(latestGoalDetail.timeBound).time
                        
                        Log.d("GoalAdapter", "Raw timestamps - Min: $minTimestamp, Max: $maxDate")
                        Log.d("GoalAdapter", "Formatted dates - Min: ${Date(minTimestamp)}, Max: ${Date(maxDate)}")

                        // Configure X-axis
                        val xAxis = lineChart.xAxis
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(true)
                        xAxis.setDrawAxisLine(true)
                        xAxis.setDrawLabels(true)
                        xAxis.textSize = 12f
                        xAxis.textColor = Color.BLACK
                        xAxis.labelRotationAngle = 45f
                        
                        // Calculate number of days and set label count
                        val daysBetween = ((maxDate - minTimestamp) / (24 * 60 * 60 * 1000)).toInt()
                        val labelCount = minOf(daysBetween + 1, 7) // Show at most 7 labels
                        xAxis.setLabelCount(labelCount, true)
                        
                        // Set X-axis limits
                        xAxis.axisMinimum = 0f
                        xAxis.axisMaximum = daysBetween.toFloat()
                        
                        Log.d("GoalAdapter", "Days between: $daysBetween, Label count: $labelCount")
                        
                        // Set X-axis formatter with explicit date calculation
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return try {
                                    val dayOffset = value.toInt()
                                    val calendar = Calendar.getInstance()
                                    calendar.time = Date(minTimestamp)
                                    calendar.add(Calendar.DAY_OF_MONTH, dayOffset)
                                    
                                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                                    val month = calendar.get(Calendar.MONTH) + 1
                                    val formattedDate = "$month/$day"
                                    Log.d("GoalAdapter", "Formatting day offset: $dayOffset -> $formattedDate")
                                    formattedDate
                                } catch (e: Exception) {
                                    Log.e("GoalAdapter", "Error formatting date", e)
                                    ""
                                }
                            }
                        }

                        // Update entries with day-based x values
                        val normalizedEntries = entries.map { entry ->
                            val dayOffset = ((entry.x - minTimestamp) / (24 * 60 * 60 * 1000)).toFloat()
                            Entry(dayOffset, entry.y)
                        }
                        dataSet.values = normalizedEntries

                        // Configure Y-axis
                        val yAxis = lineChart.axisLeft
                        yAxis.setDrawGridLines(true)
                        yAxis.setDrawAxisLine(true)
                        yAxis.setDrawLabels(true)
                        yAxis.textSize = 12f
                        yAxis.textColor = Color.BLACK
                        yAxis.axisMinimum = 0f
                        yAxis.axisMaximum = 100f
                        yAxis.granularity = 10f

                        // Disable right axis
                        lineChart.axisRight.isEnabled = false

                        // Calculate visible range
                        val visibleRange = when {
                            daysBetween <= 7 -> daysBetween.toFloat()
                            else -> 7f
                        }

                        // Set visible range and move to latest entry
                        lineChart.setVisibleXRange(1f, visibleRange)
                        lineChart.moveViewToX(normalizedEntries.last().x)

                        // Log entry values
                        normalizedEntries.forEachIndexed { index, entry ->
                            Log.d("GoalAdapter", "Entry $index - X: ${entry.x}, Y: ${entry.y}")
                        }

                        // Force update
                        lineChart.notifyDataSetChanged()
                        lineChart.invalidate()

                        Log.d("GoalAdapter", "Chart configuration completed")
                    } else {
                        Log.e("GoalAdapter", "No data available for chart")
                        lineChart.clear()
                        lineChart.invalidate()
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
                        AlertDialog.Builder(itemView.context, R.style.RoundedDialog)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete this goal?")
                            .setPositiveButton("Delete") { _, _ ->
                                Log.d(
                                    "GoalAdapter",
                                    "Delete button clicked for specificId: ${latestGoalDetail.specificId}"
                                )
                                viewModel.deleteGoalsBySpecificId(latestGoalDetail.specificId)
                            }.setNegativeButton("Cancel", null).show()
                    }

                    // Edit button functionality
                    editButton.setOnClickListener {
                        Log.d(
                            "GoalAdapter",
                            "Edit button clicked for specificId: ${latestGoalDetail.specificId}"
                        )
                        viewModel.showEditDialog(itemView.context, latestGoalDetail)
                    }

                    // Success button functionality with congratulatory dialog
                    successButton.setOnClickListener {
                        Log.d(
                            "GoalAdapter",
                            "Success button clicked for specificId: ${latestGoalDetail.specificId}"
                        )
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

        val dialog =
            AlertDialog.Builder(context, R.style.RoundedDialog).setView(dialogView).create()

        buttonSave.setOnClickListener {
            val specificText = editSpecificText.text.toString()
            val measurable = editMeasurableSeekbar.progress
            val year = editTimeBoundDatePicker.year
            val month = editTimeBoundDatePicker.month
            val day = editTimeBoundDatePicker.dayOfMonth
            val timeBound = "$year-${month + 1}-$day"

            updateGoalDetail(goalDetail.specificId, specificText, measurable, timeBound)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun updateGoalDetail(specificId: Int, specificText: String, measurable: Int, timeBound: String) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
        refreshData()
    }
}