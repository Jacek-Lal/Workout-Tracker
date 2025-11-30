package com.example.inz.utils

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.example.inz.models.WorkoutPlan
import com.example.inz.views.fragments.StartWorkoutFragment
import com.google.android.flexbox.FlexboxLayout

object BindingAdapters {

    @JvmStatic
    @BindingAdapter(value = ["workoutPlan", "currentWorkoutIndex", "handler"], requireAll = true)
    fun setWorkoutButtons(
        flexboxLayout: FlexboxLayout,
        workoutPlan: WorkoutPlan,
        currentWorkoutIndex: Int,
        handler: StartWorkoutFragment.Handler
    ) {
        flexboxLayout.removeAllViews()
        val context = flexboxLayout.context

        for ((index, phase) in workoutPlan.plan.withIndex()) {
            val button = Button(context).apply {
                text = phase.name
                tag = workoutPlan.name
                setOnClickListener {
                    handler.onWorkoutButtonClicked(workoutPlan, index)
                }

                if (index == currentWorkoutIndex) {
                    setBackgroundColor(Color.DKGRAY)
                    setTextColor(Color.LTGRAY)
                } else {
                    setBackgroundColor(Color.LTGRAY)
                    setTextColor(Color.DKGRAY)
                }

                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
            }

            flexboxLayout.addView(button)
        }
    }

    @JvmStatic
    @BindingAdapter("exercises")
    fun setExercises(
        linearLayout: LinearLayout,
        exercises: List<String>?
    ) {
        linearLayout.removeAllViews()
        val context = linearLayout.context

        exercises?.forEach { exercise ->
            val textView = TextView(context).apply {
                textSize = 16f
                setPadding(0, 0, 0, 30)
                setTextColor(Color.WHITE)
                text = exercise
            }
            linearLayout.addView(textView)
        }
    }

    @JvmStatic
    @BindingAdapter("android:text")
    fun setFloatInText(view: EditText, value: Float) {
        val currentText = view.text.toString()
        val newText = value.toString()
        if (currentText != newText) {
            view.setText(newText)
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "android:text")
    fun getFloatFromText(view: EditText): Float {
        return view.text.toString().toFloatOrNull() ?: 0f
    }

    @JvmStatic
    @BindingAdapter("android:textAttrChanged")
    fun setFloatListeners(view: EditText, listener: InverseBindingListener?) {
        if (listener == null) {
            return
        }
        view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                listener.onChange()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    @JvmStatic
    @BindingAdapter("android:text")
    fun setIntInText(view: EditText, value: Int) {
        val currentText = view.text.toString()
        val newText = value.toString()
        if (currentText != newText) {
            view.setText(newText)
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "android:text")
    fun getIntFromText(view: EditText): Int {
        return view.text.toString().toIntOrNull() ?: 0
    }

    @JvmStatic
    @BindingAdapter("android:textAttrChanged")
    fun setIntListeners(view: EditText, attrChange: InverseBindingListener?) {
        if (attrChange == null) return
        view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                attrChange.onChange()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

}
