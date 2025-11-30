package com.example.inz.models

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

data class WorkoutPlan(
    val name: String = "",
    val plan: List<WorkoutPhase> = listOf()
): BaseObservable() {

    @get:Bindable
    var currentWorkoutIndex: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.currentWorkoutIndex)
            notifyPropertyChanged(BR.currentExercises)
        }

    @get:Bindable
    val currentExercises: List<String>
        get() = plan.getOrNull(currentWorkoutIndex)?.exercises ?: emptyList()
}

data class WorkoutPhase(
    val name : String = "",
    val exercises: List<String> = listOf()
)
