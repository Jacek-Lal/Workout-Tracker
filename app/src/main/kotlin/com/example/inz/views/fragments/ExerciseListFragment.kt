package com.example.inz.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inz.views.adapters.ExerciseListAdapter
import com.example.inz.R
import com.example.inz.models.Exercise
import com.example.inz.repositories.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ExerciseListFragment : DialogFragment() {
    private var mColumnCount = 1
    private var mListener: OnExerciseSelectedListener? = null
    private var adapter: ExerciseListAdapter? = null
    private var exerciseList: List<Exercise> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
        arguments?.let {
            mColumnCount = it.getInt(ARG_COLUMN_COUNT, 1)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.fragment_list)
        recyclerView.layoutManager = if (mColumnCount <= 1) {
            LinearLayoutManager(context)
        } else {
            GridLayoutManager(context, mColumnCount)
        }

        val searchView = view.findViewById<SearchView>(R.id.searchBar)
        setupSearchView(searchView)

        loadExercises(recyclerView)
        return view
    }

    private fun loadExercises(recyclerView: RecyclerView) {
        lifecycleScope.launch {
            exerciseList = withContext(Dispatchers.IO) {
                ExerciseRepository().getAllExercises()
            }

            adapter = ExerciseListAdapter(exerciseList, mListener!!)
            recyclerView.adapter = adapter
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = when {
            parentFragment is OnExerciseSelectedListener -> parentFragment as OnExerciseSelectedListener
            context is OnExerciseSelectedListener -> context
            else -> throw RuntimeException("$context must implement OnExerciseSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    fun displayRecommendedExercises(recommendedExercises: List<Exercise>) {
        view?.findViewById<View>(R.id.loadingContainer)?.visibility = View.GONE
        val container = view?.findViewById<LinearLayout>(R.id.recommendedExercises) ?: return

        recommendedExercises.forEach { ex ->
            val exView = LayoutInflater.from(container.context)
                .inflate(R.layout.fragment_item, container, false)
            exView.findViewById<TextView>(R.id.content).text = ex.name
            exView.findViewById<TextView>(R.id.muscles).text = ex.primaryMuscle

            exView.setOnClickListener {
                mListener?.onExerciseSelected(ex.name)
            }
            container.addView(exView)
        }
    }

    interface OnExerciseSelectedListener {
        fun onExerciseSelected(exerciseName: String?)
    }

    private fun setupSearchView(searchView: SearchView) {
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterExercises(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterExercises(it) }
                return true
            }
        })
    }

    private fun filterExercises(query: String) {
        val filteredList = exerciseList.filter {
            it.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
        }
        adapter?.updateExercises(filteredList)
    }

    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"

        fun newInstance(columnCount: Int) =
                ExerciseListFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}
