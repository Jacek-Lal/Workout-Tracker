package com.example.inz.repositories

import android.util.Log

class WorkoutStatsRepository {

    private val workoutRepository = WorkoutRepository()
    private val exerciseRepository = ExerciseRepository()

    suspend fun getExerciseFrequency(): Map<String, Int> {
        return try {
            val workouts = workoutRepository.getAllWorkouts()

            workouts
                .flatMap { it.exerciseList }
                .groupingBy { it.name }
                .eachCount()

        } catch (e: Exception) {
            Log.e("WorkoutAggRepo", "Error fetching exercise frequency", e)
            emptyMap()
        }
    }

    suspend fun getMuscleLastWorkedTimestamps(): Map<String, Long> {
        return try {
            val workouts = workoutRepository.getAllWorkouts()
            val muscleLastWorkedMap = mutableMapOf<String, Long>()

            val exerciseNames = workouts
                .flatMap { it.exerciseList }
                .map { it.name }
                .toMutableSet()

            val exercises = exerciseRepository.getSpecificExercises(exerciseNames.toList())

            val exerciseToMuscleMap = mutableMapOf<String, String>()
            for (ex in exercises) {
                exerciseToMuscleMap[ex.name] = ex.primaryMuscle.trim()
            }

            for (workout in workouts) {
                val endTimeMillis = workout.endTime.time

                for (exercise in workout.exerciseList) {
                    val muscle = exerciseToMuscleMap[exercise.name] ?: continue
                    if (muscle.isEmpty()) continue

                    val currentLastWorked = muscleLastWorkedMap[muscle] ?: 0L
                    if (endTimeMillis > currentLastWorked) {
                        muscleLastWorkedMap[muscle] = endTimeMillis
                    }
                }
            }

            muscleLastWorkedMap

        } catch (e: Exception) {
            Log.e("WorkoutAggRepo", "Error fetching muscle last worked timestamps", e)
            emptyMap()
        }
    }

}
