package com.example.tick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tick.ui.TaskListScreen
import com.example.tick.viewmodel.TaskViewModel
import com.example.tick.ui.theme.TickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TickTheme {
                val taskViewModel: TaskViewModel = viewModel()
                Surface {
                    TaskListScreen(taskViewModel)
                }
            }
        }
    }
}
