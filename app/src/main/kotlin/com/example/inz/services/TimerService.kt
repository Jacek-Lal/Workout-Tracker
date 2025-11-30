package com.example.inz.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.inz.views.activities.MainActivity
import com.example.inz.R
import java.util.Locale

class TimerService : Service() {

    private val binder: IBinder = TimerBinder()
    private val handler = Handler(Looper.getMainLooper())
    var workoutStartTime: Long = 0L
    var workoutElapsedTime: Long = 0L
        private set
        get() = if (isWorkoutTimerRunning) {
            SystemClock.elapsedRealtime() - workoutStartTime
        } else {
            field
        }
    private var isWorkoutTimerRunning: Boolean = false

    var restStartTime: Long = 0L
    var restElapsedTime: Long = 0L
        private set
        get() = if (isWorkoutTimerRunning) {
            SystemClock.elapsedRealtime() - restStartTime
        } else {
            field
        }
    var isRestTimerRunning: Boolean = false
        private set
    var restDurationMillis: Long = 0L

    private val timerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isWorkoutTimerRunning) {
                workoutElapsedTime = SystemClock.elapsedRealtime() - workoutStartTime
            }

            if (isRestTimerRunning) {
                restElapsedTime = SystemClock.elapsedRealtime() - restStartTime
                if (restElapsedTime >= restDurationMillis) {
                    stopRestTimer()
                    Log.d(TAG, "Rest period completed.")
                }
            }

            updateNotification()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    inner class TimerBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "TimerService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isWorkoutTimerRunning) {
            startWorkoutTimer()
        }
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        Log.d(TAG, "TimerService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startWorkoutTimer() {
        if (!isWorkoutTimerRunning) {
            workoutStartTime = SystemClock.elapsedRealtime() - workoutElapsedTime
            isWorkoutTimerRunning = true
            handler.post(timerRunnable)
            Log.d(TAG, "Workout timer started.")
        }
    }

    fun pauseWorkoutTimer() {
        if (isWorkoutTimerRunning) {
            workoutElapsedTime = SystemClock.elapsedRealtime() - workoutStartTime
            isWorkoutTimerRunning = false
            Log.d(TAG, "Workout timer paused at $workoutElapsedTime ms.")
        }
    }

    fun resetWorkoutTimer() {
        workoutStartTime = SystemClock.elapsedRealtime()
        workoutElapsedTime = 0L
        Log.d(TAG, "Workout timer reset.")
    }

    fun startRestTimer(durationMillis: Long) {
        restDurationMillis = durationMillis
        restStartTime = SystemClock.elapsedRealtime()
        restElapsedTime = 0L
        isRestTimerRunning = true
        Log.d(TAG, "Rest timer started for $restDurationMillis ms.")
    }

    fun stopRestTimer() {
        isRestTimerRunning = false
        restElapsedTime = 0L
        restDurationMillis = 0L
        Log.d(TAG, "Rest timer stopped.")
        updateNotification()
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                pendingIntentFlags
        )

        val notificationContent = buildNotificationContent()

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Workout Timer")
                .setContentText("Workout in progress...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
                .build()
    }

    private fun buildNotificationContent(): String {
        val sb = StringBuilder("Workout in progress...")
        if (isWorkoutTimerRunning) {
            val workoutTime = formatElapsedTime(workoutElapsedTime)
            sb.append("\nWorkout Time: $workoutTime")
        }
        if (isRestTimerRunning) {
            val restRemainingTime = restDurationMillis - restElapsedTime
            val restTimeFormatted = formatElapsedTime(restRemainingTime)
            sb.append("\nRest Time Remaining: $restTimeFormatted")
        }
        return sb.toString()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Workout Timer",
                    NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for workout and rest timers."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        }
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val totalSeconds = (elapsedTime / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun terminateService() {
        handler.removeCallbacks(timerRunnable)
        stopForeground(true)
        stopSelf()
        isWorkoutTimerRunning = false
        isRestTimerRunning = false
        Log.d(TAG, "TimerService terminated.")
    }

    companion object {
        private const val CHANNEL_ID = "WorkoutTimerChannel"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_MS = 1000L
        private const val TAG = "TimerService"
    }
}
