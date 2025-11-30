package com.example.inz.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.inz.databinding.FragmentWorkoutDetailsBinding
import com.example.inz.databinding.WorkoutDetailsExerciseBinding
import com.example.inz.databinding.WorkoutDetailsSetBinding
import com.example.inz.models.ExerciseRecord
import com.example.inz.models.SetRecord
import com.example.inz.models.WorkoutRecord
import com.example.inz.viewmodels.WorkoutDetailsViewModel

class WorkoutDetailsFragment : DialogFragment() {

    private val viewModel: WorkoutDetailsViewModel by viewModels()
    private var _binding: FragmentWorkoutDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_WORKOUT_ID = "workout_id"

        fun newInstance(workoutId: String): WorkoutDetailsFragment {
            return WorkoutDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORKOUT_ID, workoutId)
                }
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWorkoutDetailsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val workoutId = arguments?.getString(ARG_WORKOUT_ID) ?: return

        viewModel.init()
        viewModel.loadWorkout(workoutId)

        viewModel.workout.observe(viewLifecycleOwner) { workout ->
            workout?.let {
                displayWorkoutDetails(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.8).toInt()
            window.setLayout(width, height)
            window.setDimAmount(0.5f)
        }
    }

    private fun displayWorkoutDetails(workout: WorkoutRecord) {
        val exercisesContainer = binding.exercisesContainer

        exercisesContainer.removeAllViews()

        workout.exerciseList.forEach { exercise ->
            addExerciseRow(exercisesContainer, exercise)
        }
    }

    private fun addExerciseRow(container: LinearLayout, exercise: ExerciseRecord) {
        val exerciseBinding = WorkoutDetailsExerciseBinding.inflate(layoutInflater, container, false)
        exerciseBinding.exercise = exercise
        exerciseBinding.lifecycleOwner = viewLifecycleOwner

        val setsContainer = exerciseBinding.setsContainer

        setsContainer.removeAllViews()

        exercise.setList.forEach { set ->
            addSetRow(setsContainer, set)
        }

        container.addView(exerciseBinding.root)
    }

    private fun addSetRow(container: LinearLayout, set: SetRecord) {
        val setBinding = WorkoutDetailsSetBinding.inflate(layoutInflater, container, false)
        setBinding.set = set
        setBinding.lifecycleOwner = viewLifecycleOwner

        container.addView(setBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
