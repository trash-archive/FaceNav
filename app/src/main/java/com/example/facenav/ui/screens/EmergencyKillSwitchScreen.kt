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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.facenav.service.EmergencyKillSwitch
import com.example.facenav.service.FaceNavAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyKillSwitchScreen(navController: NavController) {
    val context = LocalContext.current

    // Use the singleton from the running service when available;
    // fall back to a fresh instance (for settings editing when service is idle).
    val killSwitch = remember { EmergencyKillSwitch(context) }

    // Live detection state – updates whenever the service fires activate() / resume()
    val isRunning by killSwitch.isActive.collectAsState()

    // Local UI state for the three toggles
    var settings by remember { mutableStateOf(killSwitch.getSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Kill Switch")
                        Text(
                            "Multiple ways to stop instantly",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Status Hero Card ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                (if (isRunning) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error).copy(alpha = 0.18f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = if (isRunning) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = if (isRunning) "Detection Active" else "Detection Stopped",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = if (isRunning)
                            "FaceNav is running. Use any method below to stop gesture detection instantly."
                        else
                            "FaceNav has been stopped. Tap Resume to restart gesture detection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = (if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // Manual stop / resume button
                    Button(
                        onClick = {
                            if (isRunning) killSwitch.activate("Manual Button")
                            else killSwitch.resume()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Stop Now" else "Resume Detection",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Stop Methods ──────────────────────────────────────────────────
            Text(
                text = "Stop Methods",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 1. Triple Blink
            KillSwitchMethodCard(
                icon = Icons.Default.RemoveRedEye,
                title = "Triple Blink",
                description = "Blink 3 times within 2 seconds to stop immediately",
                enabled = settings.tripleBlinkEnabled,
                onToggle = { on ->
                    val s = settings.copy(tripleBlinkEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                },
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary
            )

            // 2. Voice Command
            KillSwitchMethodCard(
                icon = Icons.Default.Mic,
                title = "Voice Command",
                description = "Say a command to stop or resume FaceNav",
                enabled = settings.voiceCommandEnabled,
                onToggle = { on ->
                    val s = settings.copy(voiceCommandEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                },
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                extraContent = {
                    if (settings.voiceCommandEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Stop Commands:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                CommandChip("\"Stop FaceNav\"")
                                CommandChip("\"Emergency Stop\"")
                                CommandChip("\"Halt FaceNav\"")
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Resume Commands:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                CommandChip("\"Resume FaceNav\"")
                                CommandChip("\"Start FaceNav\"")
                            }
                        }
                    }
                }
            )

            // 3. Notification Button
            KillSwitchMethodCard(
                icon = Icons.Default.Notifications,
                title = "Notification Button",
                description = "Tap \"Emergency Stop\" in the notification shade at any time",
                enabled = settings.notificationEnabled,
                onToggle = { on ->
                    val s = settings.copy(notificationEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                },
                iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            // ── Info Card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "How It Works",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Any enabled method stops gesture detection instantly and triggers a vibration pulse. " +
                                    "Resume from the notification shade, the button above, or via a voice resume command. " +
                                    "Keep at least one method enabled at all times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun KillSwitchMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    iconBackgroundColor: Color,
    iconTint: Color,
    extraContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (enabled) iconBackgroundColor
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (enabled) iconTint
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            extraContent?.invoke()
        }
    }
}

@Composable
fun CommandChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}