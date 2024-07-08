package com.developerjp.jieungoalsettingapp.ui.dashboard

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.data.GoalDetail

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    val dbHelper = DBHelper(application)
    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    private val _newGoalList = MutableLiveData<List<GoalDetail>>()
    val newGoalList: LiveData<List<GoalDetail>> get() = _newGoalList

    init {
        _newGoalList.value = fetchGoalsFromDatabase()
    }

    private fun fetchGoalsFromDatabase(): List<GoalDetail> {
        val goalDetails = dbHelper.allGoalDetailsWithSpecificText
        val updatedList = mutableListOf<GoalDetail>()

        for (detail in goalDetails) {
            val newGoalDetail = GoalDetail(
                detail.id,
                detail.specificId,
                detail.measurable,
                detail.timeBound,
                detail.timestamp,
                detail.specificText

            )

            updatedList.add(newGoalDetail)
        }

        return updatedList
    }

    fun addGoal(goalDetail: GoalDetail) {
        val specificId = dbHelper.insertSpecific(goalDetail.specificText)
        val timestamp = System.currentTimeMillis()

        dbHelper.insertGoalDetail(
            specificId.toInt(),
            goalDetail.measurable,
            goalDetail.timeBound,
            timestamp
        )
        _newGoalList.value = fetchGoalsFromDatabase()
    }


    class GoalAdapter(private var goalDetails: List<GoalDetail>, private val dbHelper: DBHelper) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val goalDetails = goalDetails[position]
            holder.bind(goalDetails, dbHelper)
        }

        override fun getItemCount(): Int {
            return goalDetails.size
        }

        // Method to update the list of goals
        fun updateGoalDetails(newGoalDetails: List<GoalDetail>) {
            goalDetails = newGoalDetails
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val specificTextView: TextView = itemView.findViewById(R.id.specific_text)
            private val measurableTextView: TextView = itemView.findViewById(R.id.measurable_text)
            private val timeBoundTextView: TextView = itemView.findViewById(R.id.time_bound_text)

            fun bind(goal: GoalDetail, dbHelper: DBHelper) {
                specificTextView.text = goal.specificText

                // Fetch GoalDetail using the specificId
                val goalDetail = dbHelper.getGoalDetailBySpecificId(goal.id)

                // If goalDetail is not null, bind its data to the views
                if (goalDetail != null) {
                    measurableTextView.text = goalDetail.measurable.toString()
                    timeBoundTextView.text = goalDetail.timeBound
                } else {
                    measurableTextView.text = ""
                    timeBoundTextView.text = ""
                }
            }
        }
    }
}

