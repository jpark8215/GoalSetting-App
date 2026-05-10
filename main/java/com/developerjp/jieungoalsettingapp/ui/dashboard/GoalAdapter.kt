package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.GoalDetail
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalAdapter(
    private var groupedGoalDetails: Map<Int, List<GoalDetail>>,
    private val viewModel: DashboardViewModel
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    private val expandedChartSpecificIds = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val specificId = groupedGoalDetails.keys.elementAt(position)
        val goalDetails = groupedGoalDetails[specificId] ?: listOf()
        holder.bind(
            goalDetails,
            specificId,
            expandedChartSpecificIds.contains(specificId)
        )
    }

    override fun getItemCount(): Int = groupedGoalDetails.size

    fun updateGoalDetails(newGoalDetails: Map<Int, List<GoalDetail>>) {
        val nextIds = newGoalDetails.keys
        expandedChartSpecificIds.retainAll { it in nextIds }
        groupedGoalDetails = newGoalDetails
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val specificTextView: TextView = itemView.findViewById(R.id.specific_text)
        private val timeBoundTextView: TextView = itemView.findViewById(R.id.time_bound_text)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val progressSeekBar: SeekBar = itemView.findViewById(R.id.goal_progress_seekbar)
        private val progressPercentage: TextView = itemView.findViewById(R.id.progress_percentage)
        private val progressChart: BarChart = itemView.findViewById(R.id.progress_chart)
        private val chartContainer: FrameLayout = itemView.findViewById(R.id.chart_container)
        private val historyToggleRow: View = itemView.findViewById(R.id.history_toggle_row)
        private val expandIcon: ImageView = itemView.findViewById(R.id.icon_expand_chart)

        private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        private fun parseDate(dateString: String?): java.util.Date {
            if (dateString.isNullOrEmpty()) {
                return java.util.Date()
            }

            return try {
                dbDateFormat.parse(dateString)!!
            } catch (e: java.text.ParseException) {
                try {
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(dateString)
                } catch (e2: java.text.ParseException) {
                    java.util.Date()
                }
            }
        }

        fun bind(
            goalDetails: List<GoalDetail>,
            specificId: Int,
            expanded: Boolean
        ) {
            if (goalDetails.isEmpty()) return

            val sortedGoalDetails = goalDetails.sortedByDescending { it.timestamp }
            val latestGoalDetail = sortedGoalDetails.first()
            val baselineMeasurable = latestGoalDetail.measurable

            historyToggleRow.contentDescription =
                itemView.context.getString(R.string.item_goal_toggle_history_desc)

            specificTextView.text = latestGoalDetail.specificText
            timeBoundTextView.text = dateFormat.format(parseDate(latestGoalDetail.timeBound))

            progressSeekBar.progress = baselineMeasurable
            progressPercentage.text = itemView.context.getString(
                R.string.progress_percent_format,
                baselineMeasurable
            )

            chartContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            expandIcon.rotation = if (expanded) 180f else 0f

            if (expanded) {
                setupChart(sortedGoalDetails, latestGoalDetail)
            }

            historyToggleRow.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val sid = groupedGoalDetails.keys.elementAt(pos)
                if (expandedChartSpecificIds.contains(sid)) {
                    expandedChartSpecificIds.remove(sid)
                } else {
                    expandedChartSpecificIds.add(sid)
                }
                notifyItemChanged(pos)
            }

            progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        progressPercentage.text = itemView.context.getString(
                            R.string.progress_percent_format,
                            progress
                        )
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar?.progress ?: 0
                    val ctx = itemView.context
                    MaterialAlertDialogBuilder(ctx, R.style.MaterialAlertDialog_Rounded)
                        .setTitle(ctx.getString(R.string.update_progress_title))
                        .setMessage(
                            ctx.getString(R.string.update_progress_message_format, progress)
                        )
                        .setPositiveButton(ctx.getString(R.string.action_yes)) { _, _ ->
                            viewModel.updateGoalProgress(latestGoalDetail.specificId, progress)
                            if (progress == 100) {
                                viewModel.showCongratulationsDialog(ctx) {
                                    viewModel.refreshData()
                                }
                            }
                        }
                        .setNegativeButton(ctx.getString(R.string.action_no)) { _, _ ->
                            progressSeekBar.progress = baselineMeasurable
                            progressPercentage.text = ctx.getString(
                                R.string.progress_percent_format,
                                baselineMeasurable
                            )
                        }
                        .setCancelable(false)
                        .show()
                }
            })

            deleteButton.setOnClickListener {
                viewModel.showDeleteConfirmation(
                    itemView.context,
                    latestGoalDetail.specificId
                )
            }

            editButton.setOnClickListener {
                viewModel.showEditDialog(itemView.context, latestGoalDetail)
            }
        }

        private fun setupChart(goalDetails: List<GoalDetail>, latestGoalDetail: GoalDetail) {
            val ctx = progressChart.context
            val barColor = ContextCompat.getColor(ctx, R.color.primaryDarkColor)
            val labelColor = ContextCompat.getColor(ctx, R.color.textPrimary)

            val calendar = Calendar.getInstance()
            val minTimestamp = goalDetails.minByOrNull { it.timestamp }?.timestamp?.time ?: 0L
            calendar.timeInMillis = minTimestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfEarliestDay = calendar.timeInMillis

            val entries = goalDetails.map { goalDetail ->
                calendar.timeInMillis = goalDetail.timestamp.time
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val goalDayStart = calendar.timeInMillis

                val dayOffset =
                    ((goalDayStart - startOfEarliestDay) / (24 * 60 * 60 * 1000)).toFloat() + 0.5f
                BarEntry(dayOffset, goalDetail.measurable.toFloat())
            }

            if (entries.isEmpty()) {
                progressChart.clear()
                progressChart.invalidate()
                return
            }

            val dataSet = BarDataSet(entries, "%").apply {
                color = barColor
                valueTextColor = labelColor
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }

            val barData = BarData(dataSet)
            barData.barWidth = 0.9f
            progressChart.data = barData

            progressChart.description.isEnabled = false
            progressChart.legend.isEnabled = false
            progressChart.setDrawGridBackground(false)
            progressChart.setDrawBorders(false)
            progressChart.setScaleEnabled(true)
            progressChart.setPinchZoom(true)
            progressChart.setTouchEnabled(true)
            progressChart.setDragEnabled(true)

            val xAxis = progressChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.setDrawAxisLine(true)
            xAxis.setDrawLabels(true)
            xAxis.textSize = 12f
            xAxis.textColor = labelColor
            xAxis.labelRotationAngle = 45f

            val maxDate = parseDate(latestGoalDetail.timeBound).time
            val daysBetween = ((maxDate - startOfEarliestDay) / (24 * 60 * 60 * 1000)).toInt()
            val labelCount = minOf(daysBetween + 1, 7)
            xAxis.setLabelCount(labelCount, true)

            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = daysBetween.toFloat() + 1f

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

            val yAxis = progressChart.axisLeft
            yAxis.setDrawGridLines(true)
            yAxis.setDrawAxisLine(true)
            yAxis.setDrawLabels(true)
            yAxis.textSize = 12f
            yAxis.textColor = labelColor
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            yAxis.granularity = 10f

            progressChart.axisRight.isEnabled = false

            val visibleRange = when {
                daysBetween <= 7 -> daysBetween.toFloat()
                else -> 7f
            }

            progressChart.setVisibleXRange(1f, visibleRange)
            progressChart.moveViewToX(entries.last().x)

            progressChart.notifyDataSetChanged()
            progressChart.invalidate()
        }
    }
}
