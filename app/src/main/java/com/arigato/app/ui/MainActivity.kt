package com.arigato.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arigato.app.data.local.datastore.UserPreferences
import com.arigato.app.ui.navigation.ArigatoNavGraph
import com.arigato.app.ui.navigation.Screen
import com.arigato.app.ui.theme.ArigatoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by userPreferences.isDarkMode.collectAsState(initial = true)
            ArigatoTheme(darkTheme = isDarkMode) {
                ArigatoApp()
            }
        }
    }
}

@Composable
private fun ArigatoApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ArigatoBottomNavigation(navController)
        }
    ) { innerPadding ->
        ArigatoNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun ArigatoBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideOnRoutes = listOf(
        Screen.Splash.route,
        Screen.ToolDetail.route,
        Screen.Execution.route,
        Screen.Workflow.route,
        "tool/{toolId}",
        "execute/{toolId}",
        "workflow/{workflowId}"
    )
    val shouldHide = hideOnRoutes.any { currentRoute?.startsWith(it.replace("{toolId}", "").replace("{workflowId}", "")) == true }

    if (!shouldHide) {
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentRoute == Screen.Home.route,
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                label = { Text("Tools") },
                selected = currentRoute?.startsWith("tools") == true,
                onClick = {
                    navController.navigate(Screen.ToolList.withCategory(null))
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentRoute == Screen.Settings.route,
                onClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}
