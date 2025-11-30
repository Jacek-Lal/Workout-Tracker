package com.example.inz.views.fragments

import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.inz.databinding.ComponentWorkoutBinding
import com.example.inz.databinding.FragmentHistoryBinding
import com.example.inz.models.ExerciseRecord
import com.example.inz.models.WorkoutRecord
import com.example.inz.viewmodels.HistoryViewModel
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init()
        viewModel.loadWorkouts()

        viewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            displayWorkouts(workouts)
        }
    }

    private fun displayWorkouts(workouts: List<WorkoutRecord>) {
        val inflater = LayoutInflater.from(context)
        binding.contentContainer.removeAllViews()

        workouts.forEach { workout ->
            val workoutBinding = ComponentWorkoutBinding.inflate(inflater, binding.contentContainer, false)
            workoutBinding.workout = workout
            workoutBinding.handler = handler

            populateExerciseList(workoutBinding, workout, false)
            binding.contentContainer.addView(workoutBinding.root, 0)
        }
    }

    private fun populateExerciseList(workoutBinding: ComponentWorkoutBinding, workout: WorkoutRecord, isExpanded: Boolean) {
        val exercisesContainer = workoutBinding.exercisesContainer
        exercisesContainer.removeAllViews()

        val exerciseList = if (isExpanded) workout.exerciseList else workout.exerciseList.take(3)
        exerciseList.forEach { exercise ->
            addExerciseRow(exercisesContainer, exercise)
        }
    }

    private fun addExerciseRow(container: LinearLayout, exercise: ExerciseRecord) {
        val exerciseRecord = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(16, 0, 0, 16)
            text = "${exercise.name} x ${exercise.setList.size}"
        }
        container.addView(exerciseRecord)
    }

    inner class Handler {
        fun onWorkoutClicked(workout: WorkoutRecord) {
            val dialogFragment = WorkoutDetailsFragment.newInstance(workout.id)
            dialogFragment.show(parentFragmentManager, "WorkoutDetailsDialog")
        }

        fun onSeeMoreExercises(workout: WorkoutRecord, rootView: View) {
            val workoutBinding = DataBindingUtil.findBinding<ComponentWorkoutBinding>(rootView)

            if (workoutBinding != null) {
                populateExerciseList(workoutBinding, workout, true)
                workoutBinding.moreExercises.visibility = View.GONE
            } else {
                Log.e("HistoryFragment", "Binding is null in onSeeMoreExercises")
            }
        }

        fun formatElapsedTime(elapsedTime: Long): String {
            val seconds = (elapsedTime / 1000).toInt() % 60
            val minutes = (elapsedTime / (1000 * 60)).toInt()
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
        fun formatStartTime(startTime: Date): String {
            return SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(startTime.time).toString()
        }

    }

}
