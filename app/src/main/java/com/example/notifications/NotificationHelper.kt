package com.example.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.WorkoutScheduleEntity
import java.util.Calendar

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra("schedule_id") ?: "unknown"
        val workoutType = intent.getStringExtra("workout_type") ?: "Routine"
        val dayOfWeek = intent.getIntExtra("day_of_week", 7)
        val timeString = intent.getStringExtra("time_string") ?: "07:00"
        val reminderOffset = intent.getIntExtra("reminder_offset", 0)

        NotificationHelper.createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "workout")
            putExtra("schedule_id", scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ដល់ម៉ោងហាត់ប្រាណហើយ! ($workoutType)")
            .setContentText("ដល់ពេលសម្រាប់កម្មវិធី $workoutType របស់អ្នកហើយ! ចុចទីនេះដើម្បីចាប់ផ្តើម")
            .setStyle(NotificationCompat.BigTextStyle().bigText("ដល់ពេលសម្រាប់កម្មវិធី $workoutType របស់អ្នកហើយ! ចុចទីនេះដើម្បីចាប់ផ្តើមហាត់ប្រាណឥឡូវនេះ។"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(scheduleId.hashCode(), notification)
        } catch (_: Throwable) {}

        // Reschedule next occurrence for repeating alarm
        val nextSchedule = WorkoutScheduleEntity(
            id = scheduleId,
            dayOfWeek = dayOfWeek,
            timeString = timeString,
            workoutType = workoutType,
            enabled = true,
            reminderOffsetMinutes = reminderOffset
        )
        NotificationHelper.scheduleReminder(context, nextSchedule, notifyConfirmation = false)
    }
}

object NotificationHelper {

    const val CHANNEL_ID = "fithub_workout_reminders"
    private const val CHANNEL_NAME = "FITHUB Workout Reminders"
    private const val CHANNEL_DESC = "Reminders for scheduled workout routines"

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    enableLights(true)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            } catch (_: Throwable) {}
        }
    }

    fun sendNotificationNow(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "workout")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: Throwable) {}
    }

    fun scheduleReminder(context: Context, schedule: WorkoutScheduleEntity, notifyConfirmation: Boolean = true) {
        if (!schedule.enabled) {
            cancelReminder(context, schedule.id)
            return
        }

        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = schedule.timeString.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val isEveryday = schedule.dayOfWeek == 7 || schedule.dayOfWeek == -1
        val now = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            if (!isEveryday) {
                // In Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
                val targetDayOfWeek = schedule.dayOfWeek + 1
                val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
                var daysToAdd = targetDayOfWeek - currentDayOfWeek
                if (daysToAdd < 0) {
                    daysToAdd += 7
                }
                add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -schedule.reminderOffsetMinutes)

            // If time is now or in the past, roll forward to next recurrence
            if (timeInMillis <= now) {
                if (isEveryday) {
                    add(Calendar.DAY_OF_YEAR, 1)
                } else {
                    add(Calendar.DAY_OF_YEAR, 7)
                }
            }
        }

        val triggerAtMillis = calendar.timeInMillis

        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            putExtra("schedule_id", schedule.id)
            putExtra("workout_type", schedule.workoutType)
            putExtra("day_of_week", schedule.dayOfWeek)
            putExtra("time_string", schedule.timeString)
            putExtra("reminder_offset", schedule.reminderOffsetMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("navigate_to", "workout")
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    schedule.id.hashCode() + 1,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        if (notifyConfirmation) {
            val dayNames = listOf("អាទិត្យ", "ច័ន្ទ", "អង្គារ", "ពុធ", "ព្រហស្បតិ៍", "សុក្រ", "សៅរ៍")
            val targetLabel = if (isEveryday) "រាល់ថ្ងៃ" else "រៀងរាល់ថ្ងៃ " + dayNames.getOrElse(schedule.dayOfWeek) { "ថ្ងៃកំណត់" }
            sendNotificationNow(
                context,
                "បានកំណត់ម៉ោងរំលឹកហាត់ប្រាណ!",
                "FITHUB នឹងរំលឹកអ្នក $targetLabel នៅម៉ោង ${schedule.timeString} សម្រាប់ ${schedule.workoutType}"
            )
        }
    }

    fun cancelReminder(context: Context, scheduleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager?.cancel(pendingIntent)
        } catch (_: Throwable) {}
    }
}
