package com.example.inz.views.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.inz.R
import com.example.inz.databinding.FragmentStartWorkoutBinding
import com.example.inz.models.WorkoutPlan
import com.example.inz.viewmodels.StartWorkoutViewModel
import com.example.inz.views.activities.MainActivity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.LinearLayout
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
class StartWorkoutFragment : Fragment() {

    private var _binding: FragmentStartWorkoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StartWorkoutViewModel by viewModels()
    val handler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStartWorkoutBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.handler = handler

        viewModel.workoutPlans.observe(viewLifecycleOwner) { plans ->
            for (plan in plans) {
                val planBinding = com.example.inz.databinding.ComponentPrebuiltWorkoutBinding.inflate(inflater, binding.prebuiltWorkoutsContainer, false)
                planBinding.workoutPlan = plan
                planBinding.handler = handler
                binding.prebuiltWorkoutsContainer.addView(planBinding.root)
            }
        }

        return binding.root
    }

    inner class Handler {
        private fun startWorkout(activity: MainActivity, args: Bundle){
            val workoutFragment = WorkoutFragment().apply {
                arguments = args
            }

            activity.let {
                it.supportFragmentManager.beginTransaction()
                    .add(R.id.nav_host_fragment, workoutFragment)
                    .commitNow()

                it.fragmentMap[R.id.workout] = workoutFragment
                it.goToWorkout()
            }

            viewModel.setWorkoutInProgress(true)
        }

        fun showModal() {
            val isWorkoutInProgress = viewModel.isWorkoutInProgress.value ?: false
            val activity = requireActivity() as MainActivity

            if (isWorkoutInProgress) {
                activity.goToWorkout()
            } else {
                val dialog = buildDialog()

                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Start") { _, _ ->
                    val input = dialog.findViewById<EditText>("modal_input".hashCode())

                    var workoutName = input?.text.toString().trim()
                    if (workoutName.isEmpty())
                        workoutName = getString(R.string.default_workout_name)

                    val args: Bundle = Bundle().apply{
                        putString("workout_name", workoutName)
                    }
                    startWorkout(activity, args)
                }

                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                dialog.show()
            }
        }

        private fun buildDialog(): AlertDialog{
            val builder = AlertDialog.Builder(requireContext())

            val title = getString(R.string.workout_name_title)
            val spannableTitle = SpannableString(title)
            spannableTitle.setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                title.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setTitle(spannableTitle)

            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                val paddingInDp = 8
                val scale = resources.displayMetrics.density
                val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
                setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
                setBackgroundColor(Color.parseColor("#2A3136"))
            }

            val input = EditText(requireContext()).apply {
                id = "modal_input".hashCode()
                hint = getString(R.string.workout_name_hint)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginInDp = 8
                val scale = resources.displayMetrics.density
                val marginInPx = (marginInDp * scale + 0.5f).toInt()
                setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            }

            input.layoutParams = layoutParams
            container.addView(input)

            builder.setView(container)

            val dialog = builder.create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#2A3136")))

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
            }

            return dialog
        }

        fun onWorkoutButtonClicked(workoutPlan: WorkoutPlan, index: Int) {
            workoutPlan.currentWorkoutIndex = index
        }

        fun startPrebuiltWorkout(workoutPlan: WorkoutPlan) {
            val isWorkoutInProgress = viewModel.isWorkoutInProgress.value ?: false
            if (isWorkoutInProgress) {
                Toast.makeText(context, "Finish current workout first", Toast.LENGTH_LONG).show()
                return
            }

            val workoutPhase = workoutPlan.plan.getOrNull(workoutPlan.currentWorkoutIndex)
            if (workoutPhase == null) {
                Toast.makeText(context, "No workout available", Toast.LENGTH_SHORT).show()
                return
            }

            val exercises = workoutPhase.exercises
            val activity = requireActivity() as MainActivity
            val args = Bundle().apply {
                putString("workout_name", workoutPhase.name)
                putStringArrayList("exercises", ArrayList(exercises))
            }
            startWorkout(activity, args)
            viewModel.updateWorkoutProgress(workoutPlan.name)
        }
    }
}
