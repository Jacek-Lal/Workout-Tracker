package com.example.inz.views.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.inz.R
import com.example.inz.databinding.ComponentExerciseBinding
import com.example.inz.databinding.ComponentSetBinding
import com.example.inz.databinding.FragmentWorkoutBinding
import com.example.inz.models.ExerciseRecord
import com.example.inz.models.SetRecord
import com.example.inz.services.TimerService
import com.example.inz.viewmodels.WorkoutViewModel
import com.example.inz.views.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class WorkoutFragment : Fragment(), ExerciseListFragment.OnExerciseSelectedListener {

    private lateinit var binding: FragmentWorkoutBinding
    private val viewModel: WorkoutViewModel by viewModels()

    private var timerService: TimerService? = null
    private var isBound = false

    private val uiHandler = Handler()
    private val updateUITimer: Runnable = object : Runnable {
        override fun run() {

            timerService?.workoutElapsedTime?.let { ms ->
                viewModel.updateTimerText(ms)
            }

            if (timerService?.isRestTimerRunning == true) {
                val remaining = timerService!!.restDurationMillis - timerService!!.restElapsedTime
                if (remaining > 0) {
                    binding.btnRest.text = formatElapsedTime(remaining)
                    binding.btnRest.isEnabled = false
                } else {
                    binding.btnRest.isEnabled = true
                    viewModel.updateRestButtonText()
                }
            } else {
                binding.btnRest.isEnabled = true
                viewModel.updateRestButtonText()
            }

            uiHandler.postDelayed(this, 1000)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.service
            isBound = true
            uiHandler.post(updateUITimer)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_workout, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.view = this

        requireArguments().let { args ->
            val workoutName = args.getString("workout_name") ?: ""
            val exerciseList = args.getStringArrayList("exercises") ?: arrayListOf()

            if (workoutName.isNotEmpty()) {
                viewModel.currentWorkout.name = workoutName
                viewModel.workoutName.value = workoutName
            }

            exerciseList.forEach { name ->
                onExerciseSelected(name)
            }
        }

        binding.btnRest.setOnLongClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showRestTimeSelectionDialog()
            true
        }

        viewModel.initRecommendationAlgorithm(viewModel.currentWorkout.name)

        viewModel.saveWorkoutToPrefs()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        startTimerService()
        uiHandler.post(updateUITimer)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireActivity().unbindService(serviceConnection)
            isBound = false
        }
        uiHandler.removeCallbacks(updateUITimer)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRemoving || requireActivity().isFinishing) {
            viewModel.clearWorkoutProgressInPrefs()
            requireActivity().stopService(Intent(activity, TimerService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(updateUITimer)
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(updateUITimer)
    }

    private fun startTimerService() {
        val intent = Intent(requireContext(), TimerService::class.java)
        requireActivity().startService(intent)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startRest() {
        if (isBound && timerService?.isRestTimerRunning == false) {
            timerService?.startRestTimer(viewModel.selectedRestDuration)
            binding.btnRest.isEnabled = false
        }
    }

    private fun showRestTimeSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rest_time_picker, null)
        val numberPickerMinutes = dialogView.findViewById<NumberPicker>(R.id.numberPickerMinutes)
        val numberPickerSeconds = dialogView.findViewById<NumberPicker>(R.id.numberPickerSeconds)

        numberPickerMinutes.minValue = 0
        numberPickerMinutes.maxValue = 59
        numberPickerSeconds.minValue = 0
        numberPickerSeconds.maxValue = 59

        val totalSec = (viewModel.selectedRestDuration / 1000).toInt()
        numberPickerMinutes.value = totalSec / 60
        numberPickerSeconds.value = totalSec % 60

        val titleView = layoutInflater.inflate(R.layout.rest_dialog_title, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.RestDialogTheme)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val m = numberPickerMinutes.value
                val s = numberPickerSeconds.value
                viewModel.setSelectedRestDuration(m, s)
                Toast.makeText(requireContext(), "Rest time set to $m min $s sec", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.6).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.4).toInt()
            window.setLayout(width, height)
            window.setDimAmount(0.5f)
        }
    }

    fun showExerciseListFragment() {
        val fragment = ExerciseListFragment()
        fragment.show(childFragmentManager, "ExerciseListDialog")

        viewModel.generateExerciseRecommendations { recommended ->
            fragment.displayRecommendedExercises(recommended)
        }
    }

    override fun onExerciseSelected(exerciseName: String?) {
        val name = exerciseName.orEmpty()
        if (name.isBlank()) return

        val record = ExerciseRecord(name = name, description = "")

        val componentBinding = ComponentExerciseBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.exerciseContainer,
            false
        )
        componentBinding.exercise = record
        componentBinding.view = this
        componentBinding.root.setTag(R.id.exerciseBindingTag, componentBinding)

        val idx = binding.exerciseContainer.indexOfChild(binding.btnAddExercise)
        binding.exerciseContainer.addView(componentBinding.root, idx)

        componentBinding.btnAddSet.setOnClickListener { addSet(componentBinding) }
        componentBinding.btnAddSet.performClick()

        binding.exerciseContainer.post {
            (binding.exerciseContainer.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
        }

        viewModel.addPerformedExercise(name)

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.incrementSelectionCount(name)
        }

        setupThreeDotsMenu(componentBinding)
        (childFragmentManager.findFragmentByTag("ExerciseListDialog") as? DialogFragment)?.dismiss()
    }

    private fun setupThreeDotsMenu(exBinding: ComponentExerciseBinding) {
        exBinding.menuIcon.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.exercise_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_remove -> {
                        Toast.makeText(requireContext(), "Remove selected", Toast.LENGTH_SHORT).show()
                        removeExercise(exBinding)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun removeExercise(exBinding: ComponentExerciseBinding) {
        val exerciseName = exBinding.exercise?.name.orEmpty()
        viewModel.removePerformedExercise(exerciseName)

        val setsContainer = exBinding.setsContainer
        while (setsContainer.childCount > 0) {
            val setFrame = setsContainer.getChildAt(0) as FrameLayout
            val delBtn = setFrame.findViewById<Button>(R.id.deleteSetButton)
            removeSet(delBtn)
        }

        binding.exerciseContainer.removeView(exBinding.root)
    }

    fun addSet(exBinding: ComponentExerciseBinding) {
        val setContainerBinding = ComponentSetBinding.inflate(
            LayoutInflater.from(context),
            exBinding.setsContainer,
            false
        )

        val newSet = SetRecord(
            number = exBinding.setsContainer.childCount + 1,
            weight = 0f,
            reps = 0
        )
        setContainerBinding.setRecord = newSet

        setContainerBinding.root.setTag(R.id.setBindingTag, setContainerBinding)

        exBinding.setsContainer.addView(setContainerBinding.root)

        viewModel.updateTotalSets(1)

        val setComp = setContainerBinding.setComponent
        val deleteBtn = setContainerBinding.deleteSetButton
        setComp.setOnTouchListener(createSwipeListener(setComp, deleteBtn))

        deleteBtn.setOnClickListener { removeSet(it) }

        setupVolumeWatchers(setContainerBinding)
    }

    private fun removeSet(btn: View) {
        val parentView = btn.parent as? View

        val frameLayout = parentView as FrameLayout
        val setContainerBinding =
            frameLayout.getTag(R.id.setBindingTag) as? ComponentSetBinding ?: return

        val setRecord = setContainerBinding.setRecord
        setRecord?.let {
            val volumeToRemove = it.weight * it.reps
            viewModel.updateTotalVolume(-volumeToRemove)
        }

        viewModel.updateTotalSets(-1)

        val setsContainer = frameLayout.parent as LinearLayout
        val idx = setsContainer.indexOfChild(frameLayout)
        setsContainer.removeView(frameLayout)
        renumberSets(setsContainer, idx)
    }


    private fun renumberSets(container: LinearLayout, startIndex: Int) {
        for (i in startIndex until container.childCount) {
            val childFrame = container.getChildAt(i) as? FrameLayout ?: continue

            val setContainerBinding = childFrame.getTag(R.id.setBindingTag) as? ComponentSetBinding
                ?: continue

            val sr = setContainerBinding.setRecord
            if (sr != null) {
                sr.number = i + 1
                setContainerBinding.setNumberTextView.text = (i + 1).toString()
            }
        }
    }


    private fun setupVolumeWatchers(setBinding: ComponentSetBinding) {
        val previous = floatArrayOf(0f, 0f)
        val reCalc: () -> Unit = {
            val w = setBinding.setRecord?.weight ?: 0f
            val r = setBinding.setRecord?.reps ?: 0
            val oldVol = previous[0] * previous[1]
            val newVol = w * r
            viewModel.updateTotalVolume(newVol - oldVol)
            previous[0] = w
            previous[1] = r.toFloat()
        }

        setBinding.weightInput.setOnFocusChangeListener { _, _ -> reCalc() }
        setBinding.repsInput.setOnFocusChangeListener { _, _ -> reCalc() }
    }

    fun finishWorkout() {
        collectWorkoutData()
        viewModel.finishWorkout()

        if (isBound) {
            requireActivity().unbindService(serviceConnection)
            isBound = false
        }
        requireActivity().stopService(Intent(requireContext(), TimerService::class.java))

        val act = requireActivity() as MainActivity
        val startWorkoutFragment = StartWorkoutFragment()
        val historyFragment = HistoryFragment()

        act.fragmentMap[R.id.history] = historyFragment
        act.fragmentMap[R.id.workout] = startWorkoutFragment
        act.supportFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.nav_host_fragment, historyFragment)
            .hide(historyFragment)
            .add(R.id.nav_host_fragment, startWorkoutFragment, "startWorkoutFragment")
            .show(startWorkoutFragment)
            .commitNow()
        act.goToWorkout()

        Toast.makeText(requireContext(), "Workout Completed!", Toast.LENGTH_SHORT).show()
    }

    private fun collectWorkoutData() {
        val childCount = binding.exerciseContainer.childCount

        val exercises = mutableListOf<ExerciseRecord>()

        for (i in 0 until childCount - 1) {
            val child = binding.exerciseContainer.getChildAt(i)

            val exerciseBinding = child.getTag(R.id.exerciseBindingTag) as? ComponentExerciseBinding ?: continue
            val exerciseRecord = exerciseBinding.exercise ?: continue

            val setsContainer = exerciseBinding.setsContainer
            val setCount = setsContainer.childCount

            exerciseRecord.setList.clear()

            for (j in 0 until setCount) {
                val frame = setsContainer.getChildAt(j) as FrameLayout
                val setBinding = frame.getTag(R.id.setBindingTag) as? ComponentSetBinding ?: continue
                val setRecord = setBinding.setRecord

                if (setRecord != null && setRecord.reps != 0) {
                    exerciseRecord.setList.add(setRecord)
                }
            }
            if (exerciseRecord.setList.isNotEmpty()) {
                exercises.add(exerciseRecord)
            }
        }

        viewModel.currentWorkout.exerciseList.apply{
            clear()
            addAll(exercises)
        }
    }

    private fun formatElapsedTime(ms: Long): String {
        val min = (ms / 60000).toInt()
        val sec = ((ms / 1000) % 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    private fun createSwipeListener(target: View, deleteButton: Button): View.OnTouchListener {
        var downX = 0f
        val minDistance = 50
        var isSwiping = false

        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    isSwiping = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - downX
                    if (kotlin.math.abs(deltaX) > minDistance) {
                        isSwiping = true
                        target.translationX = deltaX
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isSwiping) {
                        val deltaX = event.x - downX
                        if (deltaX < -minDistance) {
                            toggleDeleteButton(target, deleteButton, true)
                        } else {
                            toggleDeleteButton(target, deleteButton, false)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleDeleteButton(target: View, btn: Button, show: Boolean) {
        val tx = if (show) -btn.width.toFloat() else 0f
        target.animate()
            .translationX(tx)
            .setDuration(200)
            .withEndAction { btn.isVisible = show }
            .start()
    }
}
