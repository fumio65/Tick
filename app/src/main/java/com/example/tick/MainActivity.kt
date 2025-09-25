package com.example.tick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.tick.ui.navigation.AppNavGraph
import com.example.tick.ui.theme.TickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TickTheme {
                val navController = rememberNavController()
                Surface {
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
