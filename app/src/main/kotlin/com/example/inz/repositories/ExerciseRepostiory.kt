package com.example.inz.repositories

import android.util.Log
import com.example.inz.models.Exercise
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.perf.FirebasePerformance
//import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.tasks.await


class ExerciseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val exercisesCollection = db.collection("exercises")

    suspend fun getAllExercises(): List<Exercise> {
//        val trace: Trace = FirebasePerformance.getInstance().newTrace("loadingExercises")

//        trace.start()

        val snapshot = exercisesCollection.get().await()
        val exercises = snapshot.documents.mapNotNull { it.toExercise() }

//        trace.stop()

        return exercises
    }

    suspend fun getExercisesByPrimaryMuscle(primaryMuscle: String): List<Exercise> {
        val standardizedMuscle = primaryMuscle.trim()
        val snapshot = exercisesCollection
            .whereEqualTo("primaryMuscle", standardizedMuscle)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toExercise() }
    }

    suspend fun getSpecificExercises(exerciseNames: List<String>): List<Exercise> {
        val snapshot = exercisesCollection
            .whereIn("name", exerciseNames.toList())
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toExercise() }
    }
    private fun DocumentSnapshot.toExercise(): Exercise? {
        return try {
            val id = this.getLong("id")?.toInt() ?:0
            val name = this.getString("name") ?: ""
            val primaryMuscle = this.getString("primaryMuscle") ?: ""
            val secondaryMuscle = this.getString("secondaryMuscle") ?: ""
            val equipment = this.getString("equipment")
            val type = this.getString("type") ?: ""

            Exercise(
                id = id,
                name = name,
                primaryMuscle = primaryMuscle,
                secondaryMuscle = secondaryMuscle,
                equipment = equipment,
                type = type
                )
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error converting document to Workout", e)
            null
        }
    }
}
