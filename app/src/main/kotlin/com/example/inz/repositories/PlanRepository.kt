package com.example.inz.repositories

import android.util.Log
import com.example.inz.models.WorkoutPhase
import com.example.inz.models.WorkoutPlan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class PlanRepository {

    private val db = FirebaseFirestore.getInstance()
    private val workoutPlansCollection = db.collection("plans")

    suspend fun getAllWorkoutPlans(): List<WorkoutPlan> {
        return try {
            val snapshot = workoutPlansCollection.get().await()
            snapshot.documents.mapNotNull { it.toWorkoutPlan() }
        } catch (e: Exception) {
            Log.e("WorkoutPlanRepo", "Error fetching workout plans", e)
            emptyList()
        }
    }
    suspend fun getPlanNames(): List<String> {
        return try {
            val snapshot = workoutPlansCollection.get().await()
            snapshot.documents.mapNotNull { it.getString("name")?: "" }
        } catch (e: Exception) {
            Log.e("WorkoutPlanRepo", "Error fetching workout plans", e)
            emptyList()
        }
    }

    suspend fun getWorkoutPlanByName(name: String): WorkoutPlan? {
        return try {
            val querySnapshot = workoutPlansCollection
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .await()

            querySnapshot.documents.firstOrNull()?.toWorkoutPlan()
        } catch (e: Exception) {
            Log.e("WorkoutPlanRepo", "Error fetching workout plan by name", e)
            null
        }
    }

    private fun DocumentSnapshot.toWorkoutPlan(): WorkoutPlan? {
        return try {
            val name = getString("name") ?: return null
            val planData = get("plan") as? List<*>
            val plan = planData?.mapNotNull { phaseMap ->
                (phaseMap as? Map<*, *>)?.toWorkoutPhase()
            } ?: listOf()
            WorkoutPlan(name, plan)
        } catch (e: Exception) {
            Log.e("WorkoutPlanRepo", "Error converting document to WorkoutPlan", e)
            null
        }
    }

    private fun Map<*, *>.toWorkoutPhase(): WorkoutPhase? {
        return try {
            val name = this["name"].toString()
            val exercises = this["exercises"] as? List<String> ?: listOf()
            WorkoutPhase(name, exercises)
        } catch (e: Exception) {
            Log.e("WorkoutPlanRepo", "Error converting map to WorkoutPhase", e)
            null
        }
    }
}
