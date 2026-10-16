package com.blueproof.ui.navigation

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.blueproof.ui.screens.*
import com.blueproof.viewmodel.AnalyticsViewModel
import com.blueproof.viewmodel.StudentViewModel
import com.blueproof.viewmodel.TeacherViewModel

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object StudentRegistration : Screen("student_registration")
    data object TeacherDashboard : Screen("teacher_dashboard")
    data object StudentDashboard : Screen("student_dashboard")
    data object StartSession : Screen("start_session")
    data object BleScan : Screen("ble_scan")
    data object LiveSession : Screen("live_session")
    data object AttendanceHistory : Screen("attendance_history")
    data object Analytics : Screen("analytics")
    data object SessionDetails : Screen("session_details/{sessionId}") {
        fun createRoute(sessionId: String) = "session_details/$sessionId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun BlueProofNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val teacherViewModel: TeacherViewModel = viewModel(factory = TeacherViewModel.Factory(app))
    val studentViewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory(app))
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory(app))

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            val currentStudent by studentViewModel.currentStudent.collectAsState()
            WelcomeScreen(
                onTeacherClick = { navController.navigate(Screen.TeacherDashboard.route) },
                onStudentClick = {
                    if (currentStudent != null) {
                        navController.navigate(Screen.StudentDashboard.route)
                    } else {
                        navController.navigate(Screen.StudentRegistration.route)
                    }
                }
            )
        }

        composable(Screen.StudentRegistration.route) {
            StudentRegistrationScreen(
                viewModel = studentViewModel,
                onRegistered = {
                    navController.navigate(Screen.StudentDashboard.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TeacherDashboard.route) {
            TeacherDashboardScreen(
                viewModel = teacherViewModel,
                onStartSessionClick = { navController.navigate(Screen.StartSession.route) },
                onViewLiveSessionClick = { navController.navigate(Screen.LiveSession.route) },
                onHistoryClick = { navController.navigate(Screen.AttendanceHistory.route) },
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.SessionDetails.createRoute(sessionId))
                }
            )
        }

        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                viewModel = studentViewModel,
                onJoinSessionClick = { navController.navigate(Screen.BleScan.route) },
                onHistoryClick = { navController.navigate(Screen.AttendanceHistory.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onSwitchStudentClick = {
                    navController.navigate(Screen.StudentRegistration.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                }
            )
        }

        composable(Screen.StartSession.route) {
            StartSessionScreen(
                viewModel = teacherViewModel,
                onSessionStarted = {
                    navController.navigate(Screen.LiveSession.route) {
                        popUpTo(Screen.TeacherDashboard.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BleScan.route) {
            BleScanScreen(
                viewModel = studentViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LiveSession.route) {
            LiveSessionScreen(
                viewModel = teacherViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AttendanceHistory.route) {
            val sessions by teacherViewModel.recentSessions.collectAsState()
            AttendanceHistoryScreen(
                sessions = sessions,
                isTeacher = true,
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.SessionDetails.createRoute(sessionId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SessionDetails.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            SessionDetailsScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
