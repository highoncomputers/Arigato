package com.arigato.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arigato.app.ui.screens.ExecutionScreen
import com.arigato.app.ui.screens.HistoryScreen
import com.arigato.app.ui.screens.HomeScreen
import com.arigato.app.ui.screens.SettingsScreen
import com.arigato.app.ui.screens.SplashScreen
import com.arigato.app.ui.screens.ToolDetailScreen
import com.arigato.app.ui.screens.ToolListScreen
import com.arigato.app.ui.screens.WorkflowScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object ToolList : Screen("tools?category={category}") {
        fun withCategory(category: String?) =
            if (category != null) "tools?category=$category" else "tools"
    }
    data object ToolDetail : Screen("tool/{toolId}") {
        fun withId(toolId: String) = "tool/$toolId"
    }
    data object Execution : Screen("execute/{toolId}") {
        fun withId(toolId: String) = "execute/$toolId"
    }
    data object Workflow : Screen("workflow/{workflowId}") {
        fun withId(workflowId: String) = "workflow/$workflowId"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun ArigatoNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onReady = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTools = {
                    navController.navigate(Screen.ToolList.withCategory(null))
                },
                onNavigateToTool = { toolId ->
                    navController.navigate(Screen.ToolDetail.withId(toolId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToWorkflow = { workflowId ->
                    navController.navigate(Screen.Workflow.withId(workflowId))
                }
            )
        }

        composable(
            route = "tools?category={category}",
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ToolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTool = { toolId ->
                    navController.navigate(Screen.ToolDetail.withId(toolId))
                },
                initialCategory = backStackEntry.arguments?.getString("category")
            )
        }

        composable(
            route = Screen.ToolDetail.route,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) {
            ToolDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExecute = { toolId ->
                    navController.navigate(Screen.Execution.withId(toolId))
                },
                onNavigateToTool = { toolId ->
                    navController.navigate(Screen.ToolDetail.withId(toolId))
                }
            )
        }

        composable(
            route = Screen.Execution.route,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) {
            ExecutionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Workflow.route,
            arguments = listOf(navArgument("workflowId") { type = NavType.StringType })
        ) {
            WorkflowScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
