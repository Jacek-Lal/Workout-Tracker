package com.example.inz.repositories

import android.util.Log
import com.example.inz.models.ExercisePreference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.Date

class PreferenceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val preferencesCollection = db.collection("preferences")

    suspend fun insertPreference(preference: ExercisePreference) {
        try {
            preferencesCollection.document(preference.exerciseName)
                .set(preference)
                .await()
            Log.d("ExercisePrefRepo", "Inserted/Updated preference for ${preference.exerciseName}")
        } catch (e: Exception) {
            Log.e("ExercisePrefRepo", "Error inserting/updating preference", e)
        }
    }

    suspend fun getAllPreferences(): Map<String, Int> {
        return try {
            val snapshot = preferencesCollection.get().await()
            val exercisePreferences = snapshot.documents.mapNotNull { it.toExercisePreference() }

            exercisePreferences.associate { it.exerciseName to it.selectionCount }
        } catch (e: Exception) {
            Log.e("ExercisePrefRepo", "Error fetching all preferences", e)
            emptyMap()
        }
    }

    suspend fun getPreferenceByName(exerciseName: String): ExercisePreference? {
        return try {
            val document = preferencesCollection.document(exerciseName).get().await()
            document.toExercisePreference()
        } catch (e: Exception) {
            Log.e("ExercisePrefRepo", "Error fetching preference for $exerciseName", e)
            null
        }
    }

    suspend fun incrementSelectionCount(exerciseName: String) {
        try {
            val docRef = preferencesCollection.document(exerciseName)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentCount = snapshot.getLong("selectionCount")?.toInt() ?: 0
                    transaction.update(docRef, "selectionCount", currentCount + 1)
                    transaction.update(docRef, "lastSelected", Date().time)
                } else {
                    val newPreference = ExercisePreference(
                        exerciseName = exerciseName,
                        lastSelected = Date().time,
                        selectionCount = 1,
                    )
                    transaction.set(docRef, newPreference)
                }
            }.await()
            Log.d("ExercisePrefRepo", "Incremented selection count for $exerciseName")
        } catch (e: Exception) {
            Log.e("ExercisePrefRepo", "Error incrementing selection count for $exerciseName", e)
        }
    }

    private fun DocumentSnapshot.toExercisePreference(): ExercisePreference? {
        return try {
            val exerciseName = getString("exerciseName") ?: return null
            val lastSelected = (getLong("lastSelected") ?: 0L)
            val selectionCount = (getLong("selectionCount")?.toInt() ?: 0)

            ExercisePreference(
                exerciseName = exerciseName,
                lastSelected = lastSelected,
                selectionCount = selectionCount,
            )
        } catch (e: Exception) {
            Log.e("ExercisePrefRepo", "Error converting document to ExercisePreference", e)
            null
        }
    }
}
