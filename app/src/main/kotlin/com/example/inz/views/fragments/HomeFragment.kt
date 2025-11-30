package com.example.inz.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.inz.views.components.CustomMarkerView
import com.example.inz.R
import com.example.inz.databinding.FragmentHomeBinding
import com.example.inz.viewmodels.HomeViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setupSpinner()
        observeViewModel()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init()
    }

    private fun setupSpinner() {
        val spinner = binding.timePeriodSpinner

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_period_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?,
                position: Int, id: Long
            ) {
                viewModel.onTimePeriodSelected(position, requireContext())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            updateChart(chartData)
        }

        viewModel.currentCategory.observe(viewLifecycleOwner) { category ->
            updateButtonSelection(category)
        }

        val spinner = binding.timePeriodSpinner

        viewModel.currentPeriod.observe(viewLifecycleOwner) { period ->
            val periods = resources.getStringArray(R.array.time_period_options)
            val index = periods.indexOf(period)
            if (index >= 0 && spinner.selectedItemPosition != index) {
                spinner.setSelection(index)
            }
        }
    }

    private fun updateChart(chartData: HomeViewModel.ChartData) {
        val chart = binding.chart

        val entries = chartData.entries

        if (entries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No workout data available for the selected period.")
            chart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, "").apply {
            setColors(resources.getColor(R.color.chart_bar, null))
            valueTextColor = resources.getColor(android.R.color.transparent, null)
            valueTextSize = 16f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        chart.data = barData

        chart.xAxis.apply {
            granularity = 1f
            valueFormatter = chartData.xAxisFormatter
            position = XAxis.XAxisPosition.BOTTOM
            textColor = resources.getColor(android.R.color.white, null)
            setDrawGridLines(false)
        }

        chart.axisLeft.apply {
            textColor = resources.getColor(android.R.color.white, null)
            axisMinimum = 0f
        }
        chart.axisRight.isEnabled = false

        chart.legend.isEnabled = false

        chart.description.isEnabled = false
        chart.setScaleEnabled(false)
        chart.setDragEnabled(false)
        chart.isScaleXEnabled = false
        chart.isScaleYEnabled = false
        chart.setPinchZoom(false)

        val marker = CustomMarkerView(requireContext(), R.layout.marker_view, chartData.labels, viewModel.currentCategory.value ?: "")
        chart.marker = marker
        chart.invalidate()
        chart.animateY(300)
    }

    private fun updateButtonSelection(selectedCategory: String) {
        val buttons = listOf(
            binding.buttonDuration,
            binding.buttonVolume,
            binding.buttonSets
        )

        buttons.forEach { button ->
            button.isSelected = button.text.toString() == selectedCategory
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
