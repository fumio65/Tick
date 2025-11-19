package com.example.tick

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import com.example.tick.ui.theme.TickTheme
import com.example.tick.uidesign.navigation.AppNavGraph
import com.example.tick.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------------------------------------
        // 1️⃣ REQUEST POST_NOTIFICATIONS (13+)
        // ---------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // ---------------------------------------
        // 2️⃣ REQUEST EXACT ALARM PERMISSION (12+)
        // ---------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        // ---------------------------------------
        // 3️⃣ CREATE NOTIFICATION CHANNEL
        // ---------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "task_channel"
            val name = "Task Reminders"
            val descriptionText = "Shows notifications when tasks are due"

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // ---------------------------------------
        // 4️⃣ SET CONTENT
        // ---------------------------------------
        enableEdgeToEdge()
        setContent {
            val taskViewModel: TaskViewModel = viewModel()
            val isDarkTheme = taskViewModel.isDarkTheme.collectAsState()

            TickTheme(darkTheme = isDarkTheme.value) {
                val navController = rememberNavController()
                Surface {
                    AppNavGraph(
                        navController = navController,
                        taskViewModel = taskViewModel,
                        isDarkTheme = isDarkTheme.value,
                        onToggleTheme = { taskViewModel.toggleTheme() }
                    )
                }
            }
        }
    }
}
