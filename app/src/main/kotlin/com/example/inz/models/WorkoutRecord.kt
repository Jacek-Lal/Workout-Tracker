package com.example.inz.models

import java.util.Date

data class WorkoutRecord(
        var id: String = "",
        var name: String = "New Workout",
        var startTime: Date = Date(),
        var endTime: Date = Date(),
        var sets: Int = 0,
        var volume: Float = 0f,
        var exerciseList: MutableList<ExerciseRecord> = mutableListOf()
){
        fun toWorkoutData(value: Double) : WorkoutData{
                return WorkoutData(label = startTime.toString(), value = value)
        }
}

data class ExerciseRecord(
        var name: String,
        var description: String? = null,
        var setList: MutableList<SetRecord> = mutableListOf()
)

data class SetRecord(
        var number: Int = 0,
        var weight: Float = 0f,
        var reps: Int = 0
)
data class WorkoutData(
        val label: String = "",
        val value: Double = 0.0
)