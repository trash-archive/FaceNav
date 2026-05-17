package com.example.facenav.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.facenav.data.PreferencesManager
import com.example.facenav.model.AccessibilityAction
import com.example.facenav.model.GestureMapping
import com.example.facenav.model.SensitivityLevel
import com.example.facenav.service.FaceDetectionService
import com.example.facenav.service.FaceNavAccessibilityService
import com.example.facenav.service.ServiceStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferencesManager = remember { PreferencesManager(context) }

    val settings by preferencesManager.getSettings().collectAsState(initial = null)
    val gestureMappings by preferencesManager.getAllGestureMappings().collectAsState(initial = emptyList())
    val isServiceRunning by ServiceStateHolder.isRunning.collectAsState()
    var isAccessibilityEnabled by remember {
        mutableStateOf(FaceNavAccessibilityService.isServiceEnabled())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = FaceNavAccessibilityService.isServiceEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isActive = isServiceRunning && isAccessibilityEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )) { append("Face") }
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )) { append("Nav") }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("emergency_killswitch") }) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = "Emergency Kill Switch",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.TouchApp, contentDescription = null) },
                    label = { Text("Gestures") },
                    selected = false,
                    onClick = { navController.navigate("gestures") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Science, contentDescription = null) },
                    label = { Text("Test") },
                    selected = false,
                    onClick = { navController.navigate("test") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Hero status card ──────────────────────────────────────────
            StatusHeroCard(
                isActive = isActive,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isServiceRunning = isServiceRunning,
                onToggle = {
                    if (isServiceRunning) stopFaceDetection(context)
                    else startFaceDetection(context)
                }
            )

            // ── Accessibility banner ──────────────────────────────────────
            AnimatedVisibility(
                visible = !isAccessibilityEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AccessibilityBanner(context)
            }

            // ── Active gestures preview ───────────────────────────────────
            val activeGestures = gestureMappings.filter {
                it.enabled && it.action != AccessibilityAction.NONE
            }
            if (activeGestures.isNotEmpty()) {
                ActiveGesturesSection(activeGestures)
            }

            // ── Settings summary card ─────────────────────────────────────
            settings?.let {
                SettingsSummaryCard(
                    sensitivity = it.sensitivity,
                    cooldown = it.cooldownMs,
                    haptic = it.hapticFeedback,
                    onNavigate = { navController.navigate("settings") }
                )
            }
        }
    }
}

// ── Hero Status Card ──────────────────────────────────────────────────────────

@Composable
fun StatusHeroCard(
    isActive: Boolean,
    isAccessibilityEnabled: Boolean,
    isServiceRunning: Boolean,
    onToggle: () -> Unit
) {
    val cardColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val btnColor by animateColorAsState(
        targetValue = if (isServiceRunning) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "btnColor"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status pill + icon row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(
                                text = if (isActive) "Active" else "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (isActive) "Detection\nRunning" else "Detection\nStopped",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = when {
                            !isAccessibilityEnabled -> "Accessibility service required"
                            !isServiceRunning -> "Tap Start to begin"
                            else -> "Monitoring facial gestures"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Icon badge
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Control button
            if (isAccessibilityEnabled) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "Stop Detection" else "Start Detection",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Accessibility Banner ──────────────────────────────────────────────────────

@Composable
fun AccessibilityBanner(context: Context) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Accessibility Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "FaceNav needs accessibility permission to perform tap, scroll, and navigation actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        "Open Settings",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Active Gestures Preview ───────────────────────────────────────────────────

@Composable
fun ActiveGesturesSection(mappings: List<GestureMapping>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active Gestures",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${mappings.size} mapped",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mappings.take(6)) { mapping ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.width(130.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = mapping.action.toIcon(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            mapping.gesture.getDisplayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            mapping.action.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Settings Summary Card ─────────────────────────────────────────────────────

@Composable
fun SettingsSummaryCard(
    sensitivity: SensitivityLevel,
    cooldown: Long,
    haptic: Boolean,
    onNavigate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onNavigate,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SettingItem(Icons.Outlined.Tune, "Sensitivity", sensitivity.getDisplayName())
                SettingItem(Icons.Outlined.Timer, "Cooldown", "${cooldown}ms")
                SettingItem(
                    if (haptic) Icons.Outlined.Vibration else Icons.Outlined.PhoneDisabled,
                    "Haptic",
                    if (haptic) "On" else "Off"
                )
            }
        }
    }
}

@Composable
fun SettingItem(icon: ImageVector, label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun SensitivityLevel.getDisplayName(): String = when (this) {
    SensitivityLevel.LOW -> "Low"
    SensitivityLevel.MEDIUM -> "Medium"
    SensitivityLevel.HIGH -> "High"
}

private fun startFaceDetection(context: Context) {
    context.startForegroundService(
        Intent(context, FaceDetectionService::class.java).apply {
            action = FaceDetectionService.ACTION_START
        }
    )
}

private fun stopFaceDetection(context: Context) {
    context.stopService(Intent(context, FaceDetectionService::class.java).apply {
        action = FaceDetectionService.ACTION_STOP
    })
}
