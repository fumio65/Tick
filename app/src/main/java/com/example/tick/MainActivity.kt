package com.example.tick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tick.ui.MainScreen
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
                    MainScreen(
                        viewModel = taskViewModel,
                        onAddTaskClick = {
                            // TODO: navigate to AddTaskScreen later
                            taskViewModel.addTask(
                                title = "Sample Task",
                                description = "This is a placeholder task"
                            )
                        }
                    )
                }
            }
        }
    }
}
