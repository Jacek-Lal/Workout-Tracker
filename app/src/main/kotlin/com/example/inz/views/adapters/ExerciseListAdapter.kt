package com.example.inz.views.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.inz.R
import com.example.inz.models.Exercise
import com.example.inz.views.fragments.ExerciseListFragment.OnExerciseSelectedListener

class ExerciseListAdapter(
    private var exercises: List<Exercise>,
    private val listener: OnExerciseSelectedListener
) : RecyclerView.Adapter<ExerciseListAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_item, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise, listener)
    }

    override fun getItemCount(): Int = exercises.size

    fun updateExercises(newExercises: List<Exercise>) {
        val diffCallback = ExerciseDiffCallback(exercises, newExercises)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        exercises = newExercises
        diffResult.dispatchUpdatesTo(this)
    }

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exerciseNameTextView: TextView = itemView.findViewById(R.id.content)
        private val musclesTextView: TextView = itemView.findViewById(R.id.muscles)

        fun bind(exercise: Exercise, listener: OnExerciseSelectedListener) {
            exerciseNameTextView.text = exercise.name
            musclesTextView.text = exercise.primaryMuscle

            itemView.setOnClickListener {
                listener.onExerciseSelected(exercise.name)
            }
        }
    }

    class ExerciseDiffCallback(
            private val oldList: List<Exercise>,
            private val newList: List<Exercise>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
