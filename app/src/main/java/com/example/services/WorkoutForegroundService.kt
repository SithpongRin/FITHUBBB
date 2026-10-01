package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class WorkoutForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "fithub_active_workout_channel"
        private const val CHANNEL_NAME = "Active Workout"
        private const val CHANNEL_DESC = "Shows live metrics during an ongoing workout"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.services.ACTION_START"
        const val ACTION_UPDATE = "com.example.services.ACTION_UPDATE"
        const val ACTION_PAUSE = "com.example.services.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.services.ACTION_RESUME"
        const val ACTION_STOP = "com.example.services.ACTION_STOP"

        const val EXTRA_WORKOUT_TYPE = "extra_workout_type"
        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_CALORIES = "extra_calories"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_JUMPS = "extra_jumps"
        const val EXTRA_IS_PAUSED = "extra_is_paused"

        fun startService(
            context: Context,
            type: String,
            seconds: Long = 0L,
            calories: Double = 0.0,
            distanceMeters: Double = 0.0,
            jumpCount: Int = 0
        ) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_TYPE, type)
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_CALORIES, calories)
                putExtra(EXTRA_DISTANCE, distanceMeters)
                putExtra(EXTRA_JUMPS, jumpCount)
                putExtra(EXTRA_IS_PAUSED, false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateMetrics(
            context: Context,
            type: String,
            seconds: Long,
            calories: Double,
            distanceMeters: Double = 0.0,
            jumpCount: Int = 0,
            isPaused: Boolean = false
        ) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_WORKOUT_TYPE, type)
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_CALORIES, calories)
                putExtra(EXTRA_DISTANCE, distanceMeters)
                putExtra(EXTRA_JUMPS, jumpCount)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var currentType: String = "WORKOUT"
    private var currentSeconds: Long = 0L
    private var currentCalories: Double = 0.0
    private var currentDistance: Double = 0.0
    private var currentJumps: Int = 0
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                currentType = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: "WORKOUT"
                currentSeconds = intent.getLongExtra(EXTRA_SECONDS, 0L)
                currentCalories = intent.getDoubleExtra(EXTRA_CALORIES, 0.0)
                currentDistance = intent.getDoubleExtra(EXTRA_DISTANCE, 0.0)
                currentJumps = intent.getIntExtra(EXTRA_JUMPS, 0)
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)

                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    } else {
                        0
                    }
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE -> {
                currentType = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: currentType
                currentSeconds = intent.getLongExtra(EXTRA_SECONDS, currentSeconds)
                currentCalories = intent.getDoubleExtra(EXTRA_CALORIES, currentCalories)
                currentDistance = intent.getDoubleExtra(EXTRA_DISTANCE, currentDistance)
                currentJumps = intent.getIntExtra(EXTRA_JUMPS, currentJumps)
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, isPaused)

                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, buildNotification())
            }
            ACTION_PAUSE -> {
                isPaused = true
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, buildNotification())
            }
            ACTION_RESUME -> {
                isPaused = false
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, buildNotification())
            }
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "workout")
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = currentSeconds / 60
        val seconds = currentSeconds % 60
        val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val statusText = if (isPaused) "⏸ បានផ្អាក (Paused)" else "⚡ កំពុងដំណើរការ (Active)"
        val metricsText = when (currentType) {
            "RUNNING", "WALKING" -> {
                val km = currentDistance / 1000.0
                String.format(Locale.US, "⏱ %s | 🔥 %.0f kcal | 📍 %.2f km", timeString, currentCalories, km)
            }
            "JUMPING" -> {
                String.format(Locale.US, "⏱ %s | 🔥 %.0f kcal | 🪢 %d Jumps", timeString, currentCalories, currentJumps)
            }
            else -> {
                String.format(Locale.US, "⏱ %s | 🔥 %.0f kcal", timeString, currentCalories)
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FITHUB: $currentType ($statusText)")
            .setContentText(metricsText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$metricsText\nកម្មវិធីកំពុងដំណើរការនៅផ្ទៃខាងក្រោយ"))
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_DESC
                    setShowBadge(false)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            } catch (_: Exception) {}
        }
    }
}
