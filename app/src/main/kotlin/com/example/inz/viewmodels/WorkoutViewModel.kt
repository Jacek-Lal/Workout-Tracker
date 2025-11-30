package com.example.inz.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.inz.models.Exercise
import com.example.inz.models.WorkoutRecord
import com.example.inz.repositories.PreferenceRepository
import com.example.inz.repositories.WorkoutRepository
import com.example.inz.utils.RecommendationAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    val timerText = MutableLiveData("00:00")
    val volumeText = MutableLiveData("0.00")
    val setsText = MutableLiveData("0")
    val restButtonText = MutableLiveData("00:00")
    val workoutName = MutableLiveData("")

    var currentWorkout = WorkoutRecord()
        private set

    private val performedExercises = mutableSetOf<String>()

    var selectedRestDuration: Long = 60000L
        private set

    var workoutElapsedTime: Long = 0L
    var isRestTimerRunning: Boolean = false
    var restElapsedTime: Long = 0L

    private val prefs = application.getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE)

    private var algo: RecommendationAlgorithm? = null
    var generatedExercises: List<Exercise> = emptyList()

    init {
        loadRestDurationFromPrefs()
        updateRestButtonText()
    }

    fun updateTimerText(elapsedTime: Long) {
        workoutElapsedTime = elapsedTime
        timerText.value = formatElapsedTime(elapsedTime)
    }

    fun formatElapsedTime(ms: Long): String {
        val minutes = (ms / 60000).toInt()
        val seconds = ((ms / 1000) % 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun updateRestButtonText() {
        val base = "Rest\n"
        val timeStr = formatElapsedTime(selectedRestDuration)
        restButtonText.value = timeStr
    }

    fun updateTotalSets(delta: Int) {
        currentWorkout.sets += delta
        setsText.value = currentWorkout.sets.toString()
    }

    fun updateTotalVolume(deltaVolume: Float) {
        currentWorkout.volume += deltaVolume
        volumeText.value = String.format(Locale.getDefault(), "%.2f", currentWorkout.volume)
    }

    fun setSelectedRestDuration(minutes: Int, seconds: Int) {
        selectedRestDuration = ((minutes * 60) + seconds) * 1000L
        updateRestButtonText()
        saveSelectedRestDurationToPrefs()
    }

    private fun loadRestDurationFromPrefs() {
        selectedRestDuration = prefs.getLong("restDuration", 60000)
    }

    private fun saveSelectedRestDurationToPrefs() {
        prefs.edit().putLong("restDuration", selectedRestDuration).apply()
    }

    fun addPerformedExercise(exerciseName: String) {
        performedExercises.add(exerciseName)
    }

    fun removePerformedExercise(exerciseName: String) {
        performedExercises.remove(exerciseName)
    }

    fun initRecommendationAlgorithm(workoutName: String) {
        algo = RecommendationAlgorithm(workoutName)
    }

    fun generateExerciseRecommendations(onResult: (List<Exercise>) -> Unit) {
        algo?.generateExerciseRecommendations(
            performedExercises,
            object : RecommendationAlgorithm.RecommendationCallback {
                override fun onRecommendationsGenerated(recommendedExercises: List<Exercise>) {
                    onResult(recommendedExercises)
                    generatedExercises = recommendedExercises
                }
            }
        )
    }
    suspend fun incrementSelectionCount(exerciseName: String){
        val exerciseNames = generatedExercises.map { ex -> ex.name }
        if(exerciseName in exerciseNames)
            PreferenceRepository().incrementSelectionCount(exerciseName)
    }

    fun saveWorkoutToPrefs() {
        prefs.edit().apply {
            putBoolean("isWorkoutInProgress", true)
            putFloat("activeWorkoutVolume", currentWorkout.volume)
            putInt("activeWorkoutSets", currentWorkout.sets)
            putString("activeWorkoutName", currentWorkout.name)
            apply()
        }
    }

    fun clearWorkoutProgressInPrefs() {
        prefs.edit().apply {
            putBoolean("isWorkoutInProgress", false)
            remove("activeWorkoutVolume")
            remove("activeWorkoutSets")
            remove("activeWorkoutName")
            apply()
        }
    }

    fun finishWorkout() {
        currentWorkout.endTime = Date()
        if (currentWorkout.exerciseList.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                WorkoutRepository().saveWorkout(currentWorkout)
            }
        }
        clearWorkoutProgressInPrefs()
    }
}
