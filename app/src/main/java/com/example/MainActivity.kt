package com.example

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

    private val viewModel: ExamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentScreen by viewModel.currentScreen.collectAsState()
            val branding by viewModel.appBranding.collectAsState()

            // Dynamic Anti-Screenshot, Anti-Overlay & Screen Recording Prevention via FLAG_SECURE & setHideOverlayWindows
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

    /**
     * Enforce strict anti-cheating policy:
     * If the user attempts to split screen (MultiWindow mode) while taking an exam,
     * immediately terminate and exit the exam with a violation record.
     */
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode && viewModel.currentScreen.value is AppScreen.TakeExam) {
            Log.w("MainActivity", "Multi-window split screen detected during exam! Forcing exam exit.")
            viewModel.terminateExamDueToSecurityViolation(
                "تم إنهاء الاختبار وإغلاقه فوراً بسبب محاولة تقسيم الشاشة (Split Screen)!"
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        @Suppress("DEPRECATION")
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        if (isInMultiWindowMode && viewModel.currentScreen.value is AppScreen.TakeExam) {
            Log.w("MainActivity", "Deprecated onMultiWindowModeChanged: split screen detected.")
            viewModel.terminateExamDueToSecurityViolation(
                "تم إنهاء الاختبار وإغلاقه فوراً بسبب محاولة تقسيم الشاشة (Split Screen)!"
            )
        }
    }

    /**
     * Enforce strict window focus monitoring:
     * If the app loses window focus (e.g. an overlay appeared, notification shade pulled down,
     * or another app drew over), immediately terminate the exam.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && viewModel.currentScreen.value is AppScreen.TakeExam) {
            Log.w("MainActivity", "Window lost focus during exam! Forcing exam exit.")
            viewModel.terminateExamDueToSecurityViolation(
                "تم إنهاء الاختبار فوراً بسبب فقدان تركيز الشاشة أو محاولة تشغيل تطبيق آخر فوق الامتحان!"
            )
        }
    }

    /**
     * When user taps Home or Recents to leave the app:
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.currentScreen.value is AppScreen.TakeExam) {
            Log.w("MainActivity", "User left app during exam! Forcing exam exit.")
            viewModel.terminateExamDueToSecurityViolation(
                "تم إنهاء الاختبار فوراً بسبب مغادرة التطبيق أو التبديل إلى تطبيق آخر!"
            )
        }
    }

    /**
     * In onResume, verify the app is not in split-screen mode if an exam is active
     */
    override fun onResume() {
        super.onResume()
        if (isInMultiWindowMode && viewModel.currentScreen.value is AppScreen.TakeExam) {
            viewModel.terminateExamDueToSecurityViolation(
                "تم إنهاء الاختبار لأن وضع تقسيم الشاشة نشط وغير مسموح به!"
            )
        }
    }
}
