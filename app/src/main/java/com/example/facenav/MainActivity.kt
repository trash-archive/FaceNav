package com.example.facenav

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.facenav.data.PreferencesManager
import com.example.facenav.ui.screens.*
import com.example.facenav.ui.theme.FaceNavTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Camera permission granted
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Audio permission granted for voice commands
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle notification permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Determine start destination based on first-launch flag (Issue 17).
        // runBlocking is acceptable here because it's a single lightweight
        // DataStore read done before the first frame is drawn.
        enableEdgeToEdge()
        val preferencesManager = PreferencesManager(this)
        val hasSeenOnboarding = runBlocking { preferencesManager.hasSeenOnboarding().first() }

        // Request necessary permissions
        requestPermissions()

        setContent {
            FaceNavTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FaceNavApp(startDestination = if (hasSeenOnboarding) "home" else "onboarding")
                }
            }
        }
    }

    private fun requestPermissions() {
        // Request camera permission
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        // Request audio permission for voice commands
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun FaceNavApp(startDestination: String = "home") {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("gestures") {
            GestureMappingScreen(navController)
        }
        composable("test") {
            TestModeScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable("emergency_killswitch") {
            EmergencyKillSwitchScreen(navController)
        }
    }
}