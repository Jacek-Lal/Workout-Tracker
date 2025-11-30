package com.example.inz.views.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.example.inz.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.util.Locale

class CustomMarkerView(
        context: Context,
        @LayoutRes layoutResource: Int,
        private val labels: List<String>? = null,
        private val currentCategory: String? = null
) : MarkerView(context, layoutResource) {

    private val markerLabel: TextView = findViewById(R.id.marker_label)
    private val markerText: TextView = findViewById(R.id.marker_text)
    private var animatorSet: AnimatorSet? = null

    companion object {
        private const val ANIMATION_DURATION_MS = 300L
        private const val MIN_SWIPE_DISTANCE_DP = 50
        private const val OFFSET_X_DP = -50f
        private const val OFFSET_Y_DP = -10f
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val index = e.x.toInt()

        if (labels != null && index in labels.indices) {
            val formattedValue = formatValue(e.y.toDouble())
            val markerDate = labels[index]

            resetTransformations()

            markerLabel.text = markerDate
            markerText.text = formattedValue

            animateMarker()
        } else {
            markerLabel.text = ""
            markerText.text = ""
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        val density = resources.displayMetrics.density
        return MPPointF(OFFSET_X_DP * density, OFFSET_Y_DP * density)
    }

    private fun formatValue(value: Double): String {
        return when (currentCategory) {
            "Duration" -> {
                if (value < 60) {
                    "${value.toLong()} seconds"
                } else {
                    "${(value / 60).toLong()} minutes"
                }
            }
            "Volume" -> String.format(Locale.getDefault(), "%.1f kg", value)
            "Sets" -> "${value.toLong()} sets"
            else -> String.format(Locale.getDefault(), "%.2f", value)
        }
    }

    private fun animateMarker() {
        animatorSet?.takeIf { it.isRunning }?.cancel()

        animatorSet = AnimatorSet().apply {
            val scaleX = ObjectAnimator.ofFloat(this@CustomMarkerView, View.SCALE_X, 0f, 1f)
            val scaleY = ObjectAnimator.ofFloat(this@CustomMarkerView, View.SCALE_Y, 0f, 1f)
            val alpha = ObjectAnimator.ofFloat(this@CustomMarkerView, View.ALPHA, 0f, 1f)

            playTogether(scaleX, scaleY, alpha)
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            duration = ANIMATION_DURATION_MS
            start()
        }
    }

    private fun resetTransformations() {
        this.scaleX = 0f
        this.scaleY = 0f
        this.alpha = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animatorSet?.cancel()
    }
}
