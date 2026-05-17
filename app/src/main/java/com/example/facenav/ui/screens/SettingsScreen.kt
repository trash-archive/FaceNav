package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.facenav.data.PreferencesManager
import com.example.facenav.model.SensitivityLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    val settings by preferencesManager.getSettings().collectAsState(initial = null)

    var selectedSensitivity by remember { mutableStateOf(SensitivityLevel.MEDIUM) }
    var cooldownValue by remember { mutableStateOf(500f) }
    var blinkThreshold by remember { mutableStateOf(150f) }
    var doubleBlinkWindow by remember { mutableStateOf(500f) }
    var nodReturnDelay by remember { mutableStateOf(300f) }
    var hapticFeedback by remember { mutableStateOf(true) }
    var soundFeedback by remember { mutableStateOf(false) }
    var cameraPreview by remember { mutableStateOf(true) }

    var initialised by remember { mutableStateOf(false) }
    LaunchedEffect(settings) {
        val s = settings
        if (s != null && !initialised) {
            initialised = true
            selectedSensitivity = s.sensitivity
            cooldownValue = s.cooldownMs.toFloat()
            blinkThreshold = s.blinkDurationThreshold.toFloat()
            doubleBlinkWindow = s.doubleBlinkWindowMs.toFloat()
            nodReturnDelay = s.nodReturnDelayMs.toFloat()
            hapticFeedback = s.hapticFeedback
            soundFeedback = s.soundFeedback
            cameraPreview = s.cameraPreview
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Fine-tune your experience",
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Detection ─────────────────────────────────────────────────
                SettingsSectionLabel("Detection")

                // Sensitivity
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
                            Column {
                                Text("Overall Sensitivity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "How easily gestures are detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    selectedSensitivity.getDisplayName(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SensitivityLevel.values().forEach { level ->
                                FilterChip(
                                    selected = selectedSensitivity == level,
                                    onClick = {
                                        selectedSensitivity = level
                                        scope.launch {
                                            settings?.let { preferencesManager.saveSettings(it.copy(sensitivity = level)) }
                                        }
                                    },
                                    label = {
                                        Text(
                                            level.getDisplayName(),
                                            fontWeight = if (selectedSensitivity == level) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (selectedSensitivity == level) null else
                                        FilterChipDefaults.filterChipBorder(
                                            enabled = true, selected = false,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }

                SliderCard(
                    title = "Blink Duration",
                    description = "Minimum time for a valid blink",
                    value = blinkThreshold,
                    valueRange = 100f..500f,
                    steps = 7,
                    valueLabel = "${blinkThreshold.toInt()}ms",
                    onValueChange = { blinkThreshold = it },
                    onValueChangeFinished = {
                        scope.launch { settings?.let { preferencesManager.saveSettings(it.copy(blinkDurationThreshold = blinkThreshold.toLong())) } }
                    }
                )

                SliderCard(
                    title = "Double Blink Window",
                    description = "Max time between two blinks",
                    value = doubleBlinkWindow,
                    valueRange = 300f..1000f,
                    steps = 6,
                    valueLabel = "${doubleBlinkWindow.toInt()}ms",
                    onValueChange = { doubleBlinkWindow = it },
                    onValueChangeFinished = {
                        scope.launch { settings?.let { preferencesManager.saveSettings(it.copy(doubleBlinkWindowMs = doubleBlinkWindow.toLong())) } }
                    }
                )

                SliderCard(
                    title = "Nod Return Delay",
                    description = "Prevents false opposite nod detection",
                    value = nodReturnDelay,
                    valueRange = 100f..1000f,
                    steps = 8,
                    valueLabel = "${nodReturnDelay.toInt()}ms",
                    onValueChange = { nodReturnDelay = it },
                    onValueChangeFinished = {
                        scope.launch { settings?.let { preferencesManager.saveSettings(it.copy(nodReturnDelayMs = nodReturnDelay.toLong())) } }
                    }
                )

                SliderCard(
                    title = "Cooldown Time",
                    description = "Delay between gesture detections",
                    value = cooldownValue,
                    valueRange = 100f..2000f,
                    steps = 18,
                    valueLabel = "${cooldownValue.toInt()}ms",
                    onValueChange = { cooldownValue = it },
                    onValueChangeFinished = {
                        scope.launch { settings?.let { preferencesManager.saveSettings(it.copy(cooldownMs = cooldownValue.toLong())) } }
                    }
                )

                // ── Feedback ──────────────────────────────────────────────────
                SettingsSectionLabel("Feedback")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ToggleRow(
                            icon = Icons.Outlined.Vibration,
                            title = "Haptic Feedback",
                            description = "Vibrate on gesture detection",
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                scope.launch { settings?.let { s -> preferencesManager.saveSettings(s.copy(hapticFeedback = it)) } }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ToggleRow(
                            icon = Icons.Outlined.VolumeUp,
                            title = "Sound Feedback",
                            description = "Play sound on gesture detection",
                            checked = soundFeedback,
                            onCheckedChange = {
                                soundFeedback = it
                                scope.launch { settings?.let { s -> preferencesManager.saveSettings(s.copy(soundFeedback = it)) } }
                            }
                        )
                    }
                }

                // ── Display ───────────────────────────────────────────────────
                SettingsSectionLabel("Display")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ToggleRow(
                        icon = Icons.Outlined.Videocam,
                        title = "Camera Preview",
                        description = "Show live camera feed indicator",
                        checked = cameraPreview,
                        onCheckedChange = {
                            cameraPreview = it
                            scope.launch { settings?.let { s -> preferencesManager.saveSettings(s.copy(cameraPreview = it)) } }
                        }
                    )
                }

                // ── Tip ───────────────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Start with default settings and adjust based on your needs. Higher sensitivity may increase battery usage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Reset button ──────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            preferencesManager.resetToDefaults()
                            initialised = false
                            selectedSensitivity = SensitivityLevel.MEDIUM
                            cooldownValue = 500f
                            blinkThreshold = 150f
                            doubleBlinkWindow = 500f
                            nodReturnDelay = 300f
                            hapticFeedback = true
                            soundFeedback = false
                            cameraPreview = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Section Label ─────────────────────────────────────────────────────────────

@Composable
fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Slider Card ───────────────────────────────────────────────────────────────

@Composable
fun SliderCard(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        valueLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Toggle Row ────────────────────────────────────────────────────────────────

@Composable
fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

// Keep old composable names as aliases so other screens that reference them still compile
@Composable
fun SectionHeader(icon: ImageVector, title: String) = SettingsSectionLabel(title)

@Composable
fun SliderSettingCard(
    title: String, description: String, value: Float,
    valueRange: ClosedFloatingPointRange<Float>, steps: Int, valueLabel: String,
    onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit
) = SliderCard(title, description, value, valueRange, steps, valueLabel, onValueChange, onValueChangeFinished)

@Composable
fun ToggleSettingItem(
    icon: ImageVector, title: String, description: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) = ToggleRow(icon, title, description, checked, onCheckedChange)
