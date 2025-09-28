package com.example.tick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.tick.uidesign.navigation.AppNavGraph
import com.example.tick.ui.theme.TickTheme
import com.example.tick.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
