package com.example.facenav.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.facenav.model.FacialGesture
import com.example.facenav.service.FaceDetectionService
import com.example.facenav.service.FaceNavAccessibilityService
import com.example.facenav.service.GestureEventBus
import com.example.facenav.service.ServiceStateHolder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestModeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isDetecting by ServiceStateHolder.isRunning.collectAsState()
    var detectedGestures by remember { mutableStateOf<List<GestureLog>>(emptyList()) }
    var isAccessibilityEnabled by remember { mutableStateOf(FaceNavAccessibilityService.isServiceEnabled()) }
    var showAccessibilityWarning by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                isAccessibilityEnabled = FaceNavAccessibilityService.isServiceEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        GestureEventBus.gestureEvents.collect { event ->
            if (isDetecting) {
                val newLog = GestureLog(
                    gesture = event.gesture,
                    timestamp = event.timestamp,
                    confidence = event.confidence
                )
                detectedGestures = (detectedGestures + newLog).takeLast(100)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Test Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Real-time gesture detection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Accessibility warning ─────────────────────────────────────────
            AnimatedVisibility(
                visible = !isAccessibilityEnabled && showAccessibilityWarning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Enable accessibility service to test gestures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Camera status card ────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDetecting) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDetecting) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    Icons.Outlined.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.padding(13.dp),
                                    tint = if (isDetecting) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (isDetecting) "Camera Active" else "Camera Standby",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDetecting) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (isDetecting) "Detecting facial gestures" else "Start detection to begin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDetecting)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Live / Off pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDetecting)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
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
                                            color = if (isDetecting) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Text(
                                    if (isDetecting) "LIVE" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDetecting) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Control button ────────────────────────────────────────────
                val btnColor by animateColorAsState(
                    targetValue = if (isDetecting) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    label = "btnColor"
                )
                Button(
                    onClick = {
                        if (!isAccessibilityEnabled) { showAccessibilityWarning = true; return@Button }
                        if (isDetecting) stopFaceDetection(context)
                        else { startFaceDetection(context); showAccessibilityWarning = false }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDetecting) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isDetecting) "Stop Detection" else "Start Detection",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Detection log ─────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Detection Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (detectedGestures.isNotEmpty()) {
                        Text(
                            "${detectedGestures.size} gestures captured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (detectedGestures.isNotEmpty()) {
                    TextButton(
                        onClick = { detectedGestures = emptyList() },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (detectedGestures.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.SensorsOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No gestures yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start detection and perform gestures to see them logged here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = detectedGestures.reversed(),
                        key = { "${it.gesture.name}_${it.timestamp}" }
                    ) { log ->
                        GestureLogItem(log)
                    }
                }
            }
        }
    }
}

// ── Log item ──────────────────────────────────────────────────────────────────

data class GestureLog(val gesture: FacialGesture, val timestamp: Long, val confidence: Float)

@Composable
fun GestureLogItem(log: GestureLog) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gesture icon
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = log.gesture.toMaterialIcon(),
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        log.gesture.getDisplayName(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            timeFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Confidence badge
                val confColor = when {
                    log.confidence >= 0.8f -> MaterialTheme.colorScheme.primary
                    log.confidence >= 0.6f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = confColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${(log.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = confColor
                    )
                }
            }
        }
    }
}

private fun startFaceDetection(context: Context) {
    context.startForegroundService(
        Intent(context, FaceDetectionService::class.java).apply { action = FaceDetectionService.ACTION_START }
    )
}

private fun stopFaceDetection(context: Context) {
    context.stopService(
        Intent(context, FaceDetectionService::class.java).apply { action = FaceDetectionService.ACTION_STOP }
    )
}
