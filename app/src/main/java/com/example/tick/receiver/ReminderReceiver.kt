package com.example.tick.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tick.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val title = intent.getStringExtra("task_title") ?: "Task Reminder"
        val taskId = intent.getIntExtra("task_id", 0)

        // ----------------------------------------------------
        // 🔐 Android 13+ requires POST_NOTIFICATIONS permission
        // ----------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // Permission not granted – skip showing notification
                return
            }
        }

        // 🔔 Get default notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)  // ✅ Add sound
            .setVibrate(longArrayOf(0, 500, 250, 500))  // ✅ Add vibration pattern
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // ✅ Use all defaults (sound, vibrate, lights)
            .build()

        NotificationManagerCompat.from(context).notify(taskId, notification)
    }
}