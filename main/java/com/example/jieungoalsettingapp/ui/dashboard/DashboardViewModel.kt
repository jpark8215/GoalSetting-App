package com.example.jieungoalsettingapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.jieungoalsettingapp.ui.home.Goal

class DashboardViewModel : ViewModel() {

//    // LiveData to hold a text value for the dashboard fragment
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is dashboard Fragment"
//    }
//    val text: LiveData<String> = _text

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
        // Update the LiveData with the new list of goals by adding the new goal to the existing list
        _newGoalList.value = currentList + listOf(goal)

    }

}
