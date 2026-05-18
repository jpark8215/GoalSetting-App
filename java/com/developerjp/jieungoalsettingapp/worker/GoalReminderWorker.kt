package com.developerjp.jieungoalsettingapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.developerjp.jieungoalsettingapp.MainActivity
import com.developerjp.jieungoalsettingapp.R
import com.developerjp.jieungoalsettingapp.data.DBHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoalReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DBHelper.getInstance(context)
            
            // Check if there are any active goals (progress < 100)
            var hasActiveGoals = false
            
            val db = dbHelper.readableDatabase
            val query = """
                SELECT ${DBHelper.GOAL_DETAIL_COLUMN_MEASURABLE}
                FROM ${DBHelper.TABLE_GOAL_DETAIL}
                INNER JOIN (
                    SELECT ${DBHelper.GOAL_DETAIL_COLUMN_SPECIFIC_ID}, MAX(${DBHelper.GOAL_DETAIL_COLUMN_TIMESTAMP}) as MaxTime
                    FROM ${DBHelper.TABLE_GOAL_DETAIL}
                    GROUP BY ${DBHelper.GOAL_DETAIL_COLUMN_SPECIFIC_ID}
                ) latest ON ${DBHelper.TABLE_GOAL_DETAIL}.${DBHelper.GOAL_DETAIL_COLUMN_SPECIFIC_ID} = latest.${DBHelper.GOAL_DETAIL_COLUMN_SPECIFIC_ID} 
                AND ${DBHelper.TABLE_GOAL_DETAIL}.${DBHelper.GOAL_DETAIL_COLUMN_TIMESTAMP} = latest.MaxTime
            """
            
            val cursor = db.rawQuery(query, null)
            while (cursor.moveToNext()) {
                val measurableIndex = cursor.getColumnIndex(DBHelper.GOAL_DETAIL_COLUMN_MEASURABLE)
                if (measurableIndex >= 0) {
                    val progress = cursor.getInt(measurableIndex)
                    if (progress < 100) {
                        hasActiveGoals = true
                        break
                    }
                }
            }
            cursor.close()
            
            if (hasActiveGoals) {
                sendNotification()
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, cannot show notification
                return
            }
        }

        val channelId = "goal_reminder_channel"
        val notificationManager = NotificationManagerCompat.from(context)

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Goal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly reminders to update your goals"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent that opens MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.goal) // Assuming this icon exists
            .setContentTitle("Goal Check-in")
            .setContentText("Don't forget to update the progress on your active goals!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
