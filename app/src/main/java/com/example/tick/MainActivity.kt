package com.example.tick

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.tick.ui.theme.TickTheme
import com.example.tick.uidesign.navigation.AppNavGraph
import com.example.tick.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ------------------------------------------------------
        // 1️⃣ REQUEST NOTIFICATION PERMISSION (Android 13+)
        // ------------------------------------------------------
        requestNotificationPermission()

        // ------------------------------------------------------
        // 2️⃣ REQUEST EXACT ALARM PERMISSION (Android 12+)
        // ------------------------------------------------------
        requestExactAlarmPermission()

        // ------------------------------------------------------
        // 3️⃣ CREATE NOTIFICATION CHANNEL (Oreo+)
        // ------------------------------------------------------
        createNotificationChannel()

        // ------------------------------------------------------
        // 4️⃣ SETUP UI CONTENT
        // ------------------------------------------------------
        enableEdgeToEdge()
        setContent {
            val taskViewModel: TaskViewModel = viewModel()

            // 🎨 TickTheme automatically follows system theme
            // No need to pass darkTheme parameter - it uses isSystemInDarkTheme() by default
            TickTheme {
                val navController = rememberNavController()
                Surface {
                    AppNavGraph(
                        navController = navController,
                        taskViewModel = taskViewModel
                    )
                }
            }
        }
    }

    // ------------------------------------------------------
    // 🔔 FUNCTION: Request POST_NOTIFICATIONS permission 13+
    // ------------------------------------------------------
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    // ------------------------------------------------------
    // ⏰ FUNCTION: Request Exact Alarm permission 12+
    // ------------------------------------------------------
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)

            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    // ------------------------------------------------------
    // 📢 FUNCTION: Create Notification Channel
    // ------------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_channel",
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications when tasks are due"
            }

            val notificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}