package com.developerjp.jieungoalsettingapp.ui.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import com.developerjp.jieungoalsettingapp.data.GoalDetail

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

        val groupedAllGoals = allGoals.groupBy { it.specificText }
            .map { (_, goals) ->
                goals.maxByOrNull { it.timeBound } ?: goals.first()
            }
        _totalGoals.value = groupedAllGoals.size

        val completedGoals = dbHelper.getGoalsByMeasurable(100)
        _totalCompletedGoals.value = completedGoals.size

        val groupedCompletedGoals = completedGoals.groupBy { it.specificText }
            .map { (_, goals) ->
                goals.maxByOrNull { it.timeBound } ?: goals.first()
            }
        _completedGoals.value = groupedCompletedGoals
    }


    companion object {
        class Factory(private val dbHelper: DBHelper) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
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

            fun bind(goal: GoalDetail) {
                specificTextView.text = goal.specificText
                timeBoundTextView.text = goal.timeBound
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
