package com.example.inz.models


data class ExercisePreference(
        val exerciseName: String,
        var selectionCount: Int = 0,
        var lastSelected: Long = System.currentTimeMillis()
)
