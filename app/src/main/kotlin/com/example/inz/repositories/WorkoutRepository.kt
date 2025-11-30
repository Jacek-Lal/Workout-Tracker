package com.example.inz.repositories

import android.icu.text.SimpleDateFormat
import android.util.Log
import com.example.inz.models.ExerciseRecord
import com.example.inz.models.SetRecord
import com.example.inz.models.WorkoutData
import com.example.inz.models.WorkoutRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.perf.FirebasePerformance
//import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val workoutsCollection = db.collection("workouts")

    suspend fun getAllWorkouts(): List<WorkoutRecord> {
        try {
//            val trace: Trace = FirebasePerformance.getInstance().newTrace("loadingWorkouts")

//            trace.start()
            val snapshot = workoutsCollection.get().await()
            val workouts = snapshot.documents
                .mapNotNull { it.toWorkout() }
                .sortedByDescending{ it.startTime }
                .reversed()

//            trace.stop()

            return workouts
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error fetching workouts", e)
            return emptyList()
        }
    }

    suspend fun getWorkoutById(workoutId: String): WorkoutRecord? {
        return try {
            val querySnapshot = workoutsCollection
                .whereEqualTo("id", workoutId)
                .limit(1)
                .get()
                .await()

            val document = querySnapshot.documents.firstOrNull()
            document?.toWorkout()
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error fetching workout with ID $workoutId", e)
            null
        }
    }
    fun saveWorkout(workout: WorkoutRecord){
        workoutsCollection.document().set(workout)
    }

    private suspend fun getWorkoutsSinceDate(startDate: Long) : List<WorkoutRecord>?{
        return try {
            val startTimestamp = Timestamp(Date(startDate))

            val querySnapshot = db.collection("workouts")
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toWorkout() }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error converting document to Workout", e)
            null
        }
    }

    suspend fun getDailyWorkoutDurations(startDate: Long): List<WorkoutData> {
        val workouts = getWorkoutsSinceDate(startDate) ?: return emptyList()

        val groupedByDate = workouts.groupBy { it.startTime.toLocalDateString() }

        return groupedByDate.map { (date, workoutsOnDate) ->
            val totalDurationSeconds = workoutsOnDate.sumOf {
                (it.endTime.time - it.startTime.time) / 1000.0
            }
            WorkoutData(label = date, value = totalDurationSeconds)
        }.sortedBy { it.label } // sort by date
    }

    suspend fun getDailyWorkoutVolumes(startDate: Long): List<WorkoutData> {
        val workouts = getWorkoutsSinceDate(startDate) ?: return emptyList()

        val groupedByDate = workouts.groupBy { it.startTime.toLocalDateString() }

        return groupedByDate.map { (date, workoutsOnDate) ->
            val totalVolume = workoutsOnDate.sumOf { it.volume.toDouble() }
            WorkoutData(label = date, value = totalVolume)
        }.sortedBy { it.label } // sort by date
    }

    suspend fun getDailyWorkoutSets(startDate: Long): List<WorkoutData> {
        val workouts = getWorkoutsSinceDate(startDate) ?: return emptyList()

        val groupedByDate = workouts.groupBy { it.startTime.toLocalDateString() }

        return groupedByDate.map { (date, workoutsOnDate) ->
            val totalSets = workoutsOnDate.sumOf { it.sets.toDouble() }
            WorkoutData(label = date, value = totalSets)
        }.sortedBy { it.label } // sort by date
    }

    private fun DocumentSnapshot.toWorkout(): WorkoutRecord? {
        return try {
            val id = this.getString("id") ?: ""
            val name = this.getString("name") ?: ""
            val startTime = this.getTimestamp("startTime")?.toDate()?: Date()
            val endTime = this.getTimestamp("endTime")?.toDate()?: Date()
            val volume = (this.getLong("volume") ?: 0).toFloat()
            val sets = (this.getLong("sets") ?: 0).toInt()

            val exerciseListData = this.get("exerciseList") as? List<*>
            val exerciseList : MutableList<ExerciseRecord> =
                (exerciseListData?.mapNotNull { exerciseMap ->
                    (exerciseMap as? Map<*, *>)?.toExercise()
                } ?: mutableListOf()).toMutableList()


            WorkoutRecord(
                id = id,
                name = name,
                startTime = startTime,
                endTime = endTime,
                exerciseList = exerciseList,
                volume = volume,
                sets = sets,

            )
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error converting document to Workout", e)
            null
        }
    }

    private fun Map<*, *>.toExercise(): ExerciseRecord? {
        return try {
            val name = this["name"] as? String ?: ""
            val description = this["description"] as? String ?: ""

            val setListData = this["setList"] as? MutableList<*>
            val setList : MutableList<SetRecord> = (setListData?.mapNotNull { setMap ->
                (setMap as? Map<*, *>)?.toSet()
            } ?: mutableListOf()).toMutableList()

            ExerciseRecord(
                name = name,
                description = description,
                setList = setList
            )
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error converting map to Exercise", e)
            null
        }
    }

    private fun Map<*, *>.toSet(): SetRecord? {
        return try {
            val number = (this["number"] as? Number)?.toInt() ?: 0
            val reps = (this["reps"] as? Number)?.toInt() ?: 0
            val weight = (this["weight"] as? Number)?.toFloat() ?: 0f

            SetRecord(
                number = number,
                reps = reps,
                weight = weight,
            )
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error converting map to Set", e)
            null
        }
    }
    private fun Date.toLocalDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(this)
    }
}
