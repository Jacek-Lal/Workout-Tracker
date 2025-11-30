package com.example.inz.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.inz.models.WorkoutPlan
import com.example.inz.repositories.PlanRepository
import com.example.inz.utils.WorkoutProgressTracker
import kotlinx.coroutines.launch

class StartWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val planRepository = PlanRepository()
    private val tracker = WorkoutProgressTracker(application)

    private val _workoutPlans = MutableLiveData<List<WorkoutPlan>>()
    val workoutPlans: LiveData<List<WorkoutPlan>> get() = _workoutPlans

    private val _workoutProgress = MutableLiveData<MutableMap<String, Int>>()
    val workoutProgress: LiveData<MutableMap<String, Int>> get() = _workoutProgress

    private val _isWorkoutInProgress = MutableLiveData<Boolean>()
    val isWorkoutInProgress: LiveData<Boolean> get() = _isWorkoutInProgress

    init {
        loadWorkoutPlans()
        _isWorkoutInProgress.value = tracker.isWorkoutInProgress
    }

    private fun loadWorkoutPlans() {
        viewModelScope.launch {
            val plans = planRepository.getAllWorkoutPlans()
            val progress = tracker.getProgress()

            plans.forEach { plan ->
                plan.currentWorkoutIndex = progress[plan.name] ?: 0
            }
            _workoutPlans.value = plans
        }
    }

    fun updateWorkoutProgress(planName: String) {
        val plans = _workoutPlans.value ?: return
        val plan = plans.find { it.name == planName } ?: return
        val nextIndex = (plan.currentWorkoutIndex + 1) % plan.plan.size
        plan.currentWorkoutIndex = nextIndex

        val progress = plans.associate { it.name to it.currentWorkoutIndex }.toMutableMap()
        tracker.saveProgress(progress)
    }

    fun setWorkoutInProgress(inProgress: Boolean) {
        _isWorkoutInProgress.value = inProgress
        tracker.isWorkoutInProgress = inProgress
    }

}
