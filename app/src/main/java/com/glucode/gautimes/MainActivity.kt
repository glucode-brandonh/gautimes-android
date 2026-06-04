package com.glucode.gautimes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glucode.gautimes.screens.home.HomeScreen
import com.glucode.gautimes.screens.settings.SettingsScreen
import com.glucode.gautimes.ui.theme.GautimesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GautimesTheme {
                GautimesApp()
            }
        }
    }
}

@Composable
fun GautimesApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
