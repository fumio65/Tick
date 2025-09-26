package com.example.tick.uidesign.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tick.uidesign.MainScreen
import com.example.tick.uidesign.AddTaskScreen
import com.example.tick.uidesign.EditTaskScreen
import com.example.tick.viewmodel.TaskViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object AddTask : Screen("add_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Int) = "edit_task/$taskId"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    taskViewModel: TaskViewModel = viewModel(),
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        // Main Screen
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = taskViewModel,
                onAddTaskClick = { navController.navigate(Screen.AddTask.route) },
                onEditTaskClick = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        // Add Task Screen
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                viewModel = taskViewModel,
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // Edit Task Screen
        composable(Screen.EditTask.route) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
            if (taskId != null) {
                EditTaskScreen(
                    taskId = taskId,
                    viewModel = taskViewModel,
                    onSave = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
