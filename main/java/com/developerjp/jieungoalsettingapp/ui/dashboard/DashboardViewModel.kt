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
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DBHelper.getInstance(application)

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    private val _goalList = MutableLiveData<Map<Int, List<GoalDetail>>>()
    val goalList: LiveData<Map<Int, List<GoalDetail>>> get() = _goalList

    init {
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
    }

    // Fetch all goal details from the database
    private fun fetchGoalsFromDatabase(): List<GoalDetail> {
        return dbHelper.allGoalDetailsWithSpecificText.map { detail ->
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
            specificId.toInt(),
            goalDetail.measurable,
            goalDetail.timeBound,
            timestamp
        )
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
    }

    // Delete goals by specific ID
    fun deleteGoalsBySpecificId(specificId: Int) {
        dbHelper.deleteGoalsBySpecificId(specificId)
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
    }


    class GoalAdapter(
        private var groupedGoalDetails: Map<Int, List<GoalDetail>>,
        private val viewModel: DashboardViewModel
    ) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
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


            private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Function to parse date string into Date object
            private fun parseDate(dateString: String?): Date {
                return try {
                    dateString?.let { dateFormat.parse(it) } ?: Date()
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
                    timeBoundTextView.text = latestGoalDetail.timeBound

                    // Create entries for the chart using all goal details
                    val entries = sortedGoalDetails.map { goalDetail ->
                        val timestampFloat = goalDetail.timestamp.time.toFloat()
                        Log.d("GoalAdapter", "Timestamp: ${goalDetail.timestamp}, Float: $timestampFloat, Measurable: ${goalDetail.measurable.toFloat()}")
                        Entry(timestampFloat, goalDetail.measurable.toFloat())
                    }

                    val dataSet = LineDataSet(entries, "%").apply {
                        color = Color.BLUE
                        valueTextColor = Color.BLACK
                        lineWidth = 2f
                        setDrawCircles(true)
                        setDrawCircleHole(true)
                        setDrawValues(true)
                    }

                    val lineData = LineData(dataSet)
                    lineChart.data = lineData

                    // Configure X-axis to show date
                    val xAxis = lineChart.xAxis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return try {
                                val date = Date(value.toLong())
                                val formattedDate = dateFormat.format(date)
                                Log.d("GoalAdapter", "Formatted Date: $formattedDate")
                                formattedDate
                            } catch (e: Exception) {
                                Log.e("GoalAdapter", "Error formatting date", e)
                                super.getFormattedValue(value)
                            }
                        }
                    }
                    xAxis.labelRotationAngle = 45f
                    xAxis.setDrawGridLines(true)
                    xAxis.setDrawLabels(true)
                    xAxis.granularity = 1f
                    xAxis.isGranularityEnabled = true
                    xAxis.isEnabled = true


                    // Set min and max values for X-axis based on earliest timestamp and timeBound date
                    val minTimestamp = sortedGoalDetails.minByOrNull { it.timestamp }?.timestamp?.time ?: 0L
                    val maxDate = parseDate(latestGoalDetail.timeBound).time
                    xAxis.axisMinimum = minTimestamp.toFloat()
                    xAxis.axisMaximum = maxDate.toFloat()
                    Log.d("GoalAdapter", "X Axis Min: $minTimestamp, Max: $maxDate")


                    // Configure Y-axis to show percentage from 0% to 100%
                    val yAxis = lineChart.axisLeft
                    yAxis.axisMinimum = 0f
                    yAxis.axisMaximum = 100f
                    yAxis.setDrawLabels(true)
                    yAxis.setDrawGridLines(true)
                    yAxis.setDrawAxisLine(true)

                    lineChart.axisRight.isEnabled = false
                    lineChart.description.isEnabled = false

                    val legend = lineChart.legend
                    legend.isEnabled = true
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(true)

                    lineChart.invalidate() // Refresh the chart

                    deleteButton.setOnClickListener {
//                        TODO add alert
                        viewModel.deleteGoalsBySpecificId(latestGoalDetail.specificId)
                    }

                    editButton.setOnClickListener {
                        viewModel.showEditDialog(itemView.context, latestGoalDetail)
                    }

                    successButton.setOnClickListener {
//                        TODO create congrats dialog
                        viewModel.updateGoalDetail(
                            specificId = latestGoalDetail.specificId,
                            specificText = latestGoalDetail.specificText,
                            measurable = 100,
                            timeBound = dateFormat.format(Date(System.currentTimeMillis()))
                        )
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
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

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

    private fun updateGoalDetail(
        specificId: Int,
        specificText: String,
        measurable: Int,
        timeBound: String
    ) {
        dbHelper.updateGoalDetail(specificId, specificText, measurable, timeBound)
        _goalList.value = fetchGoalsFromDatabase().groupBy { it.specificId }
    }
}

