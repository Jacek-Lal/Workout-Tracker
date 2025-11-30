package com.example.inz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inz.models.WorkoutRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.inz.repositories.WorkoutRepository

class WorkoutDetailsViewModel : ViewModel() {

    private val _workout = MutableLiveData<WorkoutRecord>()
    val workout: LiveData<WorkoutRecord> get() = _workout

    val formattedStartTime: LiveData<String> = MutableLiveData()
    val formattedDuration: LiveData<String> = MutableLiveData()
    val formattedVolume: LiveData<String> = MutableLiveData()

    private lateinit var repository: WorkoutRepository

    fun init(){
        repository = WorkoutRepository()
    }

    fun loadWorkout(workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val workoutRecord = repository.getWorkoutById(workoutId)?: return@launch

            _workout.postValue(workoutRecord)

            (formattedStartTime as MutableLiveData).postValue(formatStartTime(workoutRecord.startTime))
            (formattedDuration as MutableLiveData).postValue(formatDuration(workoutRecord.startTime, workoutRecord.endTime))
            (formattedVolume as MutableLiveData).postValue(formatVolume(workoutRecord.volume))
        }
    }

    private fun formatStartTime(date: Date): String {
        val formatter = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    private fun formatDuration(startTime: Date, endTime: Date): String {
        val durationMillis = endTime.time - startTime.time
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun formatVolume(volume: Float): String {
        return String.format(Locale.getDefault(), "%.0f kg", volume)
    }
}
