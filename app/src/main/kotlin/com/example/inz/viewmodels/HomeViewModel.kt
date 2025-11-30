package com.example.inz.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.inz.R
import com.example.inz.models.WorkoutData
import com.example.inz.repositories.WorkoutRepository
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
//import com.google.firebase.perf.FirebasePerformance
//import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_PERIOD: String = "Last 7 days"

class HomeViewModel : ViewModel() {

    private val _currentCategory = MutableLiveData("Duration")
    val currentCategory: LiveData<String> = _currentCategory

    private val _currentPeriod = MutableLiveData("Last 7 days")
    val currentPeriod: LiveData<String> = _currentPeriod

    private val _chartData = MutableLiveData<ChartData>()
    val chartData: LiveData<ChartData> = _chartData

    private lateinit var repository: WorkoutRepository

    fun init() {
        repository = WorkoutRepository()
        loadChartData()
    }

    fun onCategorySelected(category: String) {
        _currentCategory.value = category
        loadChartData()
    }

    fun onTimePeriodSelected(position: Int, context: Context) {
        val timePeriodOptions: Array<String> = context.resources.getStringArray(R.array.time_period_options)
        _currentPeriod.value = timePeriodOptions[position]
        loadChartData()
    }

    private fun loadChartData() {
        val startDate = calculateStartDate(currentPeriod.value ?: DEFAULT_PERIOD).timeInMillis

        viewModelScope.launch {
//            val trace: Trace = FirebasePerformance.getInstance().newTrace("loadingChartData")

//            trace.start()

            val data: List<WorkoutData> = withContext(Dispatchers.IO) {
                when (currentCategory.value) {
                    "Duration" -> repository.getDailyWorkoutDurations(startDate)
                    "Volume" -> repository.getDailyWorkoutVolumes(startDate)
                    "Sets" -> repository.getDailyWorkoutSets(startDate)
                    else -> emptyList()
                }
            }

            val labels = generateLabels(currentPeriod.value ?: DEFAULT_PERIOD)
            val mergedData = mergeDataWithLabels(data, labels, currentPeriod.value ?: DEFAULT_PERIOD)
            val entries = mergedData.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val xAxisFormatter = IndexAxisValueFormatter(labels)

            _chartData.value = ChartData(entries, xAxisFormatter, labels)

//            trace.stop()
        }
    }

    data class ChartData(
        val entries: List<BarEntry>,
        val xAxisFormatter: IndexAxisValueFormatter,
        val labels: List<String>
    )

    private fun calculateStartDate(selectedPeriod: String): Calendar {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "Last 7 days" -> cal.add(Calendar.DAY_OF_YEAR, -6)
            "Last 30 days" -> cal.add(Calendar.DAY_OF_YEAR, -29)
            "Last 3 months" -> {
                cal.add(Calendar.MONTH, -3)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            "Last 12 months" -> {
                cal.add(Calendar.MONTH, -11)
                cal[Calendar.DAY_OF_MONTH] = 1
            }
            else -> cal.add(Calendar.DAY_OF_YEAR, -6)
        }
        setToStartOfDay(cal)
        return cal
    }

    private fun generateLabels(selectedPeriod: String): List<String> {
        val labelsList = mutableListOf<String>()
        val today = Calendar.getInstance()
        setToStartOfDay(today)

        val dateFormat = getDateFormat(selectedPeriod)
        var cal = calculateStartDate(selectedPeriod)

        while (cal.before(today) || cal == today) {
            val label = dateFormat.format(cal.time)
            Log.d("HomeFragment", label)
            labelsList.add(label)
            cal = incrementCalendar(cal, selectedPeriod)
        }
        return labelsList
    }

    private fun mergeDataWithLabels(
        data: List<WorkoutData>,
        allLabels: List<String>,
        selectedPeriod: String
    ): List<Double> {
        val mergedData = mutableListOf<Double>()
        var currBucket = calculateStartDate(selectedPeriod)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var dateIdx = 0

        for (i in allLabels.indices) {
            var total = 0.0
            val nextBucket = incrementCalendar(currBucket, selectedPeriod)
            while (dateIdx < data.size && isDateBetween(
                    stringToCalendar(data[dateIdx].label, dateFormat),
                    currBucket,
                    nextBucket
                )
            ) {
                total += data[dateIdx].value
                dateIdx += 1
            }
            mergedData.add(total)
            currBucket = nextBucket
        }
        return mergedData
    }


    private fun incrementCalendar(cal: Calendar, selectedPeriod: String): Calendar {
        val calCopy = cal.clone() as Calendar
        when (selectedPeriod) {
            "Last 12 months" -> calCopy.add(Calendar.MONTH, 1)
            "Last 3 months" -> calCopy.add(Calendar.DAY_OF_YEAR, 7)
            "Last 7 days", "Last 30 days" -> calCopy.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calCopy
    }

    private fun isDateBetween(target: Calendar, startDate: Calendar, endDate: Calendar): Boolean {
        val targetCopy = target.clone() as Calendar
        val startCopy = startDate.clone() as Calendar
        val endCopy = endDate.clone() as Calendar

        setToStartOfDay(targetCopy)
        setToStartOfDay(startCopy)
        setToStartOfDay(endCopy)
        return (targetCopy == startCopy || targetCopy.after(startCopy)) &&
                targetCopy.before(endCopy)
    }

    private fun stringToCalendar(dateString: String, dateFormat: SimpleDateFormat): Calendar {
        dateFormat.isLenient = false
        val date = dateFormat.parse(dateString) ?: throw ParseException("Invalid date", 0)
        return Calendar.getInstance().apply {
            time = date
        }
    }

    private fun setToStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
    private fun getDateFormat(selectedPeriod: String): SimpleDateFormat {
        return when (selectedPeriod) {
            "Last 12 months" -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
            "Last 3 months", "Last 30 days", "Last 7 days" -> SimpleDateFormat("MMM dd", Locale.getDefault())
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
    }

}
