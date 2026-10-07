package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.ExamSecurityHelper
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ExamBuilderScreen
import com.example.ui.screens.ExamResultScreen
import com.example.ui.screens.ExamTakerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SubmissionDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ExamViewModel = viewModel()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val branding by viewModel.appBranding.collectAsState()

            // Dynamic Anti-Screenshot & Screen Recording Prevention via FLAG_SECURE
            LaunchedEffect(branding?.globalScreenSecurity, currentScreen) {
                val shouldProtect = (branding?.globalScreenSecurity == true) || (currentScreen is AppScreen.TakeExam)
                if (shouldProtect) {
                    ExamSecurityHelper.enableWindowSecurity(this@MainActivity)
                } else {
                    ExamSecurityHelper.disableWindowSecurity(this@MainActivity)
                }
            }

            // Global back handler for custom screen stack
            if (currentScreen !is AppScreen.Home && currentScreen !is AppScreen.TakeExam) {
                BackHandler {
                    viewModel.navigateBack()
                }
            }

            MyApplicationTheme(
                primaryHexColor = branding?.themeColorHex,
                dynamicColor = false
            ) {
                // Set RTL Layout Direction for Arabic language perfection
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (val screen = currentScreen) {
                            is AppScreen.Home -> {
                                HomeScreen(viewModel = viewModel)
                            }
                            is AppScreen.TakeExam -> {
                                ExamTakerScreen(viewModel = viewModel)
                            }
                            is AppScreen.ExamResult -> {
                                ExamResultScreen(
                                    submissionId = screen.submissionId,
                                    viewModel = viewModel
                                )
                            }
                            is AppScreen.BuildExam -> {
                                ExamBuilderScreen(viewModel = viewModel)
                            }
                            is AppScreen.AdminDashboard -> {
                                AdminDashboardScreen(viewModel = viewModel)
                            }
                            is AppScreen.SubmissionDetail -> {
                                SubmissionDetailScreen(
                                    submissionId = screen.submissionId,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
