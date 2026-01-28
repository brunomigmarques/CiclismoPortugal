package com.ciclismo.portugal.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ciclismo.portugal.presentation.admin.AdminScreen
import com.ciclismo.portugal.presentation.calendar.CalendarScreen
import com.ciclismo.portugal.presentation.details.DetailsScreen
import com.ciclismo.portugal.presentation.home.HomeScreen
import com.ciclismo.portugal.presentation.news.NewsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Provas", Icons.Default.Home)
    object News : Screen("news", "Notícias", Icons.Default.Newspaper)
    object Calendar : Screen("calendar", "Calendário", Icons.Default.CalendarMonth)
    object Details : Screen("details/{provaId}", "Detalhes") {
        fun createRoute(provaId: Long) = "details/$provaId"
    }
    object Admin : Screen("admin", "Admin")
}

val bottomNavItems = listOf(
    Screen.News,
    Screen.Home,
    Screen.Calendar
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CiclismoNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.News.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onProvaClick = { provaId ->
                        navController.navigate(Screen.Details.createRoute(provaId))
                    },
                    onAdminAccess = {
                        navController.navigate(Screen.Admin.route)
                    }
                )
            }

            composable(Screen.News.route) {
                NewsScreen()
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onProvaClick = { provaId ->
                        navController.navigate(Screen.Details.createRoute(provaId))
                    }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("provaId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val provaId = backStackEntry.arguments?.getLong("provaId") ?: 0L
                DetailsScreen(
                    provaId = provaId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Admin.route) {
                AdminScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let { icon ->
                        Icon(icon, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
