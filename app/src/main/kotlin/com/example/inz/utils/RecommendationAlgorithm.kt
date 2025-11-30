package com.example.inz.utils

import com.example.inz.models.Exercise
import com.example.inz.repositories.ExerciseRepository
import com.example.inz.repositories.PreferenceRepository
import com.example.inz.repositories.WorkoutStatsRepository
//import com.google.firebase.perf.FirebasePerformance
//import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class RecommendationAlgorithm(
        private val workoutName: String
) {
    private val MAX_RECOMMENDATIONS = 5
    private val workoutTypeMuscleMap: Map<String, List<String>> = mapOf(
            "Push" to listOf("Chest", "Shoulders", "Triceps"),
            "Pull" to listOf("Lats", "Upper Back", "Biceps"),
            "Legs" to listOf("Quadriceps", "Hamstrings", "Calves", "Glutes"),
            "Upper" to listOf("Chest", "Shoulders", "Upper Back", "Lats", "Biceps", "Triceps"),
            "Lower" to listOf("Quadriceps", "Hamstrings", "Calves", "Glutes"),
            "FBW" to listOf(
                    "Chest", "Shoulders", "Upper Back", "Lats",
                    "Biceps", "Triceps", "Quadriceps", "Hamstrings",
                    "Calves", "Glutes"
            )
    )


    interface RecommendationCallback {
        fun onRecommendationsGenerated(recommendedExercises: List<Exercise>)
    }

    fun generateExerciseRecommendations(
            performedExercises: Set<String>,
            callback: RecommendationCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
//            val trace: Trace = FirebasePerformance.getInstance().newTrace("recommendationGeneration")

//            trace.start()
            var currentWorkoutType = extractWorkoutTypeFromName(workoutName)
            if (!workoutTypeMuscleMap.containsKey(currentWorkoutType)) {
                currentWorkoutType = "FBW"
            }

            val allExercises = ExerciseRepository().getAllExercises()

            val performedExerciseEntities = allExercises.filter { performedExercises.contains(it.name) }
            val musclesWorkedCount = performedExerciseEntities
                    .map { it.primaryMuscle }
                    .groupingBy { it }
                    .eachCount()

            val candidateExercises = allExercises.filter { !performedExercises.contains(it.name) }

            val statsRepo = WorkoutStatsRepository()
            val muscleLastWorkedTimestamps = statsRepo.getMuscleLastWorkedTimestamps()
            val exerciseFrequency = statsRepo.getExerciseFrequency()

            val preferenceRepo = PreferenceRepository()
            val exerciseSelectionCounts = preferenceRepo.getAllPreferences()

            val recommendedExercises = recommendExercises(
                    candidateExercises,
                    muscleLastWorkedTimestamps,
                    exerciseFrequency,
                    exerciseSelectionCounts,
                    musclesWorkedCount,
                    currentWorkoutType
            )

            val topRecommendations = limitOneExercisePerMuscleGroup(recommendedExercises, topN = MAX_RECOMMENDATIONS)

//            trace.stop()

            withContext(Dispatchers.Main) {
                callback.onRecommendationsGenerated(topRecommendations)
            }
        }
    }

    private fun extractWorkoutTypeFromName(workoutName: String): String {
        return workoutName.split(" ").firstOrNull() ?: "FBW"
    }

    private fun limitOneExercisePerMuscleGroup(
            recommendedExercises: List<Exercise>,
            topN: Int
    ): List<Exercise> {
        val musclesIncluded = mutableSetOf<String>()
        val limitedRecommendations = mutableListOf<Exercise>()

        for (exercise in recommendedExercises) {
            val muscle = exercise.primaryMuscle
            if (musclesIncluded.add(muscle)) {
                limitedRecommendations.add(exercise)
                if (limitedRecommendations.size >= topN) break
            }
        }

        return limitedRecommendations
    }

    private fun calculateTotalScore(
            exercise: Exercise,
            muscleLastWorkedTimestamps: Map<String, Long>,
            exerciseFrequency: Map<String, Int>,
            exerciseSelectionCounts: Map<String, Int>,
            musclesWorkedCount: Map<String, Int>,
            currentWorkoutType: String
    ): Double {
        val recencyScore = calculateRecencyScore(exercise, muscleLastWorkedTimestamps)
        val frequencyScore = calculateFrequencyScore(exercise, exerciseFrequency)
        val neglectedMuscleScore = calculateNeglectedMuscleScore(exercise, muscleLastWorkedTimestamps)
        val workoutTypeScore = calculateWorkoutTypeScore(exercise, currentWorkoutType)
        val preferenceScore = calculatePreferenceScore(exercise, exerciseSelectionCounts)
        val muscleBalanceScore = calculateMuscleBalanceScore(exercise, musclesWorkedCount)

        return recencyScore * 12 +
                frequencyScore * 8 +
                neglectedMuscleScore * 8 +
                workoutTypeScore * 32 +
                preferenceScore * 8 +
                muscleBalanceScore * 32
    }

    private fun recommendExercises(
            candidateExercises: List<Exercise>,
            muscleLastWorkedTimestamps: Map<String, Long>,
            exerciseFrequency: Map<String, Int>,
            exerciseSelectionCounts: Map<String, Int>,
            musclesWorkedCount: Map<String, Int>,
            currentWorkoutType: String
    ): List<Exercise> {
        return candidateExercises.sortedByDescending { exercise ->
            calculateTotalScore(
                    exercise,
                    muscleLastWorkedTimestamps,
                    exerciseFrequency,
                    exerciseSelectionCounts,
                    musclesWorkedCount,
                    currentWorkoutType
            )
        }
    }

    private fun calculateRecencyScore(
            exercise: Exercise,
            muscleLastWorkedTimestamps: Map<String, Long>
    ): Double {
        val currentTime = System.currentTimeMillis()
        val primaryMuscleLastWorked = muscleLastWorkedTimestamps[exercise.primaryMuscle] ?: 0L
        val secondaryMuscleLastWorked = muscleLastWorkedTimestamps[exercise.secondaryMuscle] ?: 0L

        val primaryDelta = currentTime - primaryMuscleLastWorked
        val secondaryDelta = currentTime - secondaryMuscleLastWorked

        val primaryScore = normalizeTimeDelta(primaryDelta)
        val secondaryScore = normalizeTimeDelta(secondaryDelta)

        return primaryScore * 0.7 + secondaryScore * 0.3
    }

    private fun calculateNeglectedMuscleScore(
            exercise: Exercise,
            muscleLastWorkedTimestamps: Map<String, Long>
    ): Double {
        val currentTime = System.currentTimeMillis()
        val muscleLastWorked = muscleLastWorkedTimestamps[exercise.primaryMuscle] ?: 0L
        val delta = currentTime - muscleLastWorked
        return normalizeTimeDelta(delta)
    }

    private fun normalizeTimeDelta(delta: Long): Double {
        val maxDelta = 604800000.0 // 7 days in ms
        return min(1.0, delta / maxDelta)
    }

    private fun calculateFrequencyScore(
            exercise: Exercise,
            exerciseFrequency: Map<String, Int>
    ): Double {
        val frequency = exerciseFrequency[exercise.name] ?: 0
        val maxFrequency = if (exerciseFrequency.isEmpty()) 1 else exerciseFrequency.values.maxOrNull() ?: 1
        return frequency.toDouble() / maxFrequency
    }

    private fun calculateWorkoutTypeScore(
            exercise: Exercise,
            currentWorkoutType: String
    ): Double {
        val preferredMusclesInWorkout = workoutTypeMuscleMap[currentWorkoutType] ?: emptyList()
        return if (preferredMusclesInWorkout.contains(exercise.primaryMuscle)) 1.0 else 0.0
    }

    private fun  calculatePreferenceScore(
            exercise: Exercise,
            exerciseSelectionCount: Map<String, Int>
    ): Double {
        val selectionCount = exerciseSelectionCount[exercise.name] ?: 0
        val maxSelectionCount = if (exerciseSelectionCount.isEmpty()) 1 else exerciseSelectionCount.values.maxOrNull() ?: 1

        return selectionCount.toDouble() / maxSelectionCount
    }

    private fun calculateMuscleBalanceScore(
            exercise: Exercise,
            musclesWorkedCount: Map<String, Int>
    ): Double {
        val primaryMuscle = exercise.primaryMuscle
        val muscleCount = musclesWorkedCount[primaryMuscle] ?: 0

        val maxMuscleCount = musclesWorkedCount.values.maxOrNull() ?: 0

        return if (maxMuscleCount == 0) {
            1.0
        } else {
            (maxMuscleCount - muscleCount).toDouble() / maxMuscleCount
        }
    }
}
