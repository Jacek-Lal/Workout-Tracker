package com.example.inz.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.inz.repositories.PlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkoutProgressTracker(context: Context) {

    companion object {
        private const val PREFS_NAME = "WorkoutPrefs"
        private const val LAST_WORKOUT_KEY = "lastWorkout"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isWorkoutInProgress: Boolean
        get() = prefs.getBoolean("isWorkoutInProgress", false)
        set(value){ prefs.edit().putBoolean("isWorkoutInProgress", value).apply()}

    fun saveProgress(workoutProgress: Map<String, Int>) {
        prefs.edit().apply {
            workoutProgress.forEach { (key, value) ->
                putInt(key, value)
            }
            apply()
        }
    }

    val lastWorkout: String
        get() = prefs.getString(LAST_WORKOUT_KEY, "None") ?: "None"

    fun saveLastWorkout(workoutName: String) {
        prefs.edit().putString(LAST_WORKOUT_KEY, workoutName).apply()
    }
    suspend fun getProgress(): MutableMap<String, Int> = withContext(Dispatchers.IO) {
        PlanRepository().getPlanNames().associateWith { key ->
            prefs.getInt(key, 0)
        }.toMutableMap()
    }

}
