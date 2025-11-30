package com.example.inz.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inz.models.WorkoutRecord
import com.example.inz.repositories.WorkoutRepository
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private val _workouts = MutableLiveData<List<WorkoutRecord>>()
    val workouts: LiveData<List<WorkoutRecord>> get() = _workouts
    private lateinit var repository: WorkoutRepository

    val isLoading = MutableLiveData(true)

    fun init(){
        repository = WorkoutRepository()
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            val workouts = repository.getAllWorkouts()
            _workouts.postValue(workouts)
            isLoading.postValue(false)
        }
    }
}
