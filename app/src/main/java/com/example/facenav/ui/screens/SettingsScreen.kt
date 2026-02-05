package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    // Update local state when settings load
    LaunchedEffect(settings) {
        val currentSettings = settings
        if (currentSettings != null) {
            selectedSensitivity = currentSettings.sensitivity
            cooldownValue = currentSettings.cooldownMs.toFloat()
            blinkThreshold = currentSettings.blinkDurationThreshold.toFloat()
            doubleBlinkWindow = currentSettings.doubleBlinkWindowMs.toFloat()
            nodReturnDelay = currentSettings.nodReturnDelayMs.toFloat()
            hapticFeedback = currentSettings.hapticFeedback
            soundFeedback = currentSettings.soundFeedback
            cameraPreview = currentSettings.cameraPreview
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings")
                        Text(
                            "Fine-tune your experience",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Detection Settings Section
                SectionHeader(
                    icon = Icons.Default.Radar,
                    title = "Detection Settings"
                )

                // Overall Sensitivity
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overall Sensitivity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "How easily gestures are detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedSensitivity.getDisplayName(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                            settings?.let {
                                                preferencesManager.saveSettings(
                                                    it.copy(sensitivity = level)
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            level.getDisplayName(),
                                            fontWeight = if (selectedSensitivity == level)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (selectedSensitivity == level) null else
                                        FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = false,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }

                // Slider Settings
                SliderSettingCard(
                    title = "Blink Duration",
                    description = "Minimum time for a valid blink",
                    value = blinkThreshold,
                    valueRange = 100f..500f,
                    steps = 7,
                    valueLabel = "${blinkThreshold.toInt()}ms",
                    onValueChange = { blinkThreshold = it },
                    onValueChangeFinished = {
                        scope.launch {
                            settings?.let {
                                preferencesManager.saveSettings(
                                    it.copy(blinkDurationThreshold = blinkThreshold.toLong())
                                )
                            }
                        }
                    }
                )

                SliderSettingCard(
                    title = "Double Blink Window",
                    description = "Max time between two blinks",
                    value = doubleBlinkWindow,
                    valueRange = 300f..1000f,
                    steps = 6,
                    valueLabel = "${doubleBlinkWindow.toInt()}ms",
                    onValueChange = { doubleBlinkWindow = it },
                    onValueChangeFinished = {
                        scope.launch {
                            settings?.let {
                                preferencesManager.saveSettings(
                                    it.copy(doubleBlinkWindowMs = doubleBlinkWindow.toLong())
                                )
                            }
                        }
                    }
                )

                SliderSettingCard(
                    title = "Nod Return Delay",
                    description = "Prevents false opposite nod detection",
                    value = nodReturnDelay,
                    valueRange = 100f..1000f,
                    steps = 8,
                    valueLabel = "${nodReturnDelay.toInt()}ms",
                    onValueChange = { nodReturnDelay = it },
                    onValueChangeFinished = {
                        scope.launch {
                            settings?.let {
                                preferencesManager.saveSettings(
                                    it.copy(nodReturnDelayMs = nodReturnDelay.toLong())
                                )
                            }
                        }
                    }
                )

                SliderSettingCard(
                    title = "Cooldown Time",
                    description = "Delay between gesture detections",
                    value = cooldownValue,
                    valueRange = 100f..2000f,
                    steps = 18,
                    valueLabel = "${cooldownValue.toInt()}ms",
                    onValueChange = { cooldownValue = it },
                    onValueChangeFinished = {
                        scope.launch {
                            settings?.let {
                                preferencesManager.saveSettings(
                                    it.copy(cooldownMs = cooldownValue.toLong())
                                )
                            }
                        }
                    }
                )

                // Feedback Section
                SectionHeader(
                    icon = Icons.Default.Notifications,
                    title = "Feedback & Alerts"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        ToggleSettingItem(
                            icon = Icons.Default.Vibration,
                            title = "Haptic Feedback",
                            description = "Vibrate on gesture detection",
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                scope.launch {
                                    settings?.let { s ->
                                        preferencesManager.saveSettings(
                                            s.copy(hapticFeedback = it)
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        ToggleSettingItem(
                            icon = Icons.Default.VolumeUp,
                            title = "Sound Feedback",
                            description = "Play sound on gesture detection",
                            checked = soundFeedback,
                            onCheckedChange = {
                                soundFeedback = it
                                scope.launch {
                                    settings?.let { s ->
                                        preferencesManager.saveSettings(
                                            s.copy(soundFeedback = it)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Display Section
                SectionHeader(
                    icon = Icons.Default.Visibility,
                    title = "Display Options"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ToggleSettingItem(
                        icon = Icons.Default.Camera,
                        title = "Camera Preview",
                        description = "Show live camera feed indicator",
                        checked = cameraPreview,
                        onCheckedChange = {
                            cameraPreview = it
                            scope.launch {
                                settings?.let { s ->
                                    preferencesManager.saveSettings(
                                        s.copy(cameraPreview = it)
                                    )
                                }
                            }
                        }
                    )
                }

                // Info Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Performance Tips",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Start with default settings and adjust based on your needs. Higher sensitivity may increase battery usage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Bottom Reset Button
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                preferencesManager.resetToDefaults()
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
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reset to Defaults",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SliderSettingCard(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun ToggleSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (checked)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (checked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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