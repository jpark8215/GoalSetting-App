package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
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
        private val optionsButton: ImageButton = itemView.findViewById(R.id.button_options)
        private val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.goal_progress_indicator)
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

            progressIndicator.progress = baselineMeasurable
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

            optionsButton.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.goal_item_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_update_progress -> {
                            showUpdateProgressDialog(latestGoalDetail)
                            true
                        }
                        R.id.action_edit_goal -> {
                            viewModel.showEditDialog(itemView.context, latestGoalDetail)
                            true
                        }
                        R.id.action_delete_goal -> {
                            viewModel.showDeleteConfirmation(itemView.context, latestGoalDetail.specificId)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            // Also allow updating progress by clicking the card or progress area
            itemView.setOnClickListener {
                showUpdateProgressDialog(latestGoalDetail)
            }
        }

        private fun showUpdateProgressDialog(latestGoalDetail: GoalDetail) {
            val ctx = itemView.context
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_update_progress, null)
            val seekBar = dialogView.findViewById<SeekBar>(R.id.update_progress_seekbar)
            val progressText = dialogView.findViewById<TextView>(R.id.update_progress_text)

            seekBar.progress = latestGoalDetail.measurable
            progressText.text = ctx.getString(R.string.progress_percent_format, latestGoalDetail.measurable)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    progressText.text = ctx.getString(R.string.progress_percent_format, progress)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            MaterialAlertDialogBuilder(ctx, R.style.MaterialAlertDialog_Rounded)
                .setTitle(ctx.getString(R.string.update_progress_title))
                .setView(dialogView)
                .setPositiveButton(ctx.getString(R.string.action_save)) { _, _ ->
                    val newProgress = seekBar.progress
                    viewModel.updateGoalProgress(latestGoalDetail.specificId, newProgress)
                    if (newProgress == 100) {
                        viewModel.showCongratulationsDialog(ctx) {
                            viewModel.refreshData()
                        }
                    }
                }
                .setNegativeButton(ctx.getString(R.string.action_cancel), null)
                .show()
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
