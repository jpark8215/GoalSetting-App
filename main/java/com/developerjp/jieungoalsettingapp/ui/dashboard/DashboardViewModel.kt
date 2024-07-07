package com.developerjp.jieungoalsettingapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developerjp.jieungoalsettingapp.ui.home.Goal

// Declare the DashboardViewModel class, extending ViewModel
class DashboardViewModel : ViewModel() {

    // LiveData to hold a text value for the dashboard fragment
    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    // LiveData to hold a list of new goals
    private val _newGoalList = MutableLiveData<List<Goal>>()
    val newGoalList: LiveData<List<Goal>> get() = _newGoalList

    // Initialize the list with an empty list when the ViewModel is created
    init {
        _newGoalList.value = emptyList()
    }

    // Function to add a new goal to the list
    fun addGoal(goal: Goal) {
        // Get the current list of goals from the LiveData or use an empty list if it's null
        val currentList = _newGoalList.value ?: emptyList()

        // Hard-coded strings before the entered value
        val hardCodedStrings = listOf(" My Goal: ", " Achieved ", "%", " Will Achieve by: ")

        // Calculate the percentage by multiplying the measurable value by 10
        val percentage = goal.measurable.toIntOrNull()?.times(10)?.toString() ?: "0"

        // Combine the hard-coded strings and the entered value into a new goal
        val newGoal = Goal(
            hardCodedStrings[0] + goal.specific,
            hardCodedStrings[1] + percentage + hardCodedStrings[2],
             hardCodedStrings[3] + goal.timeBound
        )

        // Update the LiveData with the new list of goals by adding the new goal to the existing list
        _newGoalList.value = currentList + listOf(newGoal)
    }
}
