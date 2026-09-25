package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.auth.AuthHubScreen
import com.example.ui.components.FloatingPillNavigationBar
import com.example.ui.home.HomeScreen
import com.example.ui.student.StudentClassesScreen
import com.example.ui.student.StudentScannerDashboardScreen
import com.example.ui.teacher.TeacherHomeScreen
import com.example.ui.teacher.TeacherRosterScreen
import com.example.ui.teacher.TeacherStartClassScreen
import com.example.ui.theme.AttendanceFlowTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            AttendanceFlowTheme(darkTheme = isDarkMode) {
                AttendanceFlowApp(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun AttendanceFlowApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val showNavBar = isLoggedIn && currentScreen != AppScreen.AUTH

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 300),
                modifier = Modifier.fillMaxSize(),
                label = "ScreenNavigation"
            ) { screen ->
                when (screen) {
                    AppScreen.AUTH -> {
                        AuthHubScreen(viewModel = viewModel)
                    }
                    AppScreen.STUDENT_HOME -> {
                        HomeScreen(viewModel = viewModel)
                    }
                    AppScreen.STUDENT_SCAN -> {
                        StudentScannerDashboardScreen(viewModel = viewModel)
                    }
                    AppScreen.STUDENT_CLASSES -> {
                        StudentClassesScreen(viewModel = viewModel)
                    }
                    AppScreen.TEACHER_HOME -> {
                        TeacherHomeScreen(viewModel = viewModel)
                    }
                    AppScreen.TEACHER_START_CLASS -> {
                        TeacherStartClassScreen(viewModel = viewModel)
                    }
                    AppScreen.TEACHER_ROSTER -> {
                        TeacherRosterScreen(viewModel = viewModel)
                    }
                }
            }

            if (showNavBar) {
                FloatingPillNavigationBar(
                    userRole = currentRole,
                    currentScreen = currentScreen,
                    onScreenSelected = { selectedScreen ->
                        viewModel.navigateToScreen(selectedScreen)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

