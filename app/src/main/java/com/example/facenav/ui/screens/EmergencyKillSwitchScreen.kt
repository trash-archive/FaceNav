package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
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
    val killSwitch = remember { EmergencyKillSwitch.getOrCreate(context) }
    val isRunning by killSwitch.isActive.collectAsState()
    var settings by remember { mutableStateOf(killSwitch.getSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Stop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Multiple ways to stop instantly",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Status hero card ──────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isRunning) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Outlined.Shield else Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = if (isRunning) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onError
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isRunning) "Detection Active" else "Detection Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            if (isRunning)
                                "FaceNav is running. Use any method below to stop instantly."
                            else
                                "FaceNav has been stopped. Tap Resume to restart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = (if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer).copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            if (isRunning) killSwitch.activate("Manual Button")
                            else killSwitch.resume()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isRunning) "Stop Now" else "Resume Detection",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Stop methods label ────────────────────────────────────────────
            Text(
                "Stop Methods",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Triple Blink ──────────────────────────────────────────────────
            KillSwitchMethodCard(
                icon = Icons.Outlined.RemoveRedEye,
                title = "Triple Blink",
                description = "Blink 3 times within 2 seconds to stop immediately",
                enabled = settings.tripleBlinkEnabled,
                onToggle = { on ->
                    val s = settings.copy(tripleBlinkEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                }
            )

            // ── Voice Command ─────────────────────────────────────────────────
            KillSwitchMethodCard(
                icon = Icons.Outlined.Mic,
                title = "Voice Command",
                description = "Say a command to stop or resume FaceNav",
                enabled = settings.voiceCommandEnabled,
                onToggle = { on ->
                    val s = settings.copy(voiceCommandEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                },
                extraContent = {
                    if (settings.voiceCommandEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Stop Commands",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CommandChip("\"Stop FaceNav\"")
                                CommandChip("\"Emergency Stop\"")
                                CommandChip("\"Halt FaceNav\"")
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Resume Commands",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CommandChip("\"Resume FaceNav\"")
                                CommandChip("\"Start FaceNav\"")
                            }
                        }
                    }
                }
            )

            // ── Notification Button ───────────────────────────────────────────
            KillSwitchMethodCard(
                icon = Icons.Outlined.Notifications,
                title = "Notification Button",
                description = "Tap \"Emergency Stop\" in the notification shade at any time",
                enabled = settings.notificationEnabled,
                onToggle = { on ->
                    val s = settings.copy(notificationEnabled = on)
                    settings = s; killSwitch.updateSettings(s)
                }
            )

            // ── Info card ─────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Any enabled method stops gesture detection instantly and triggers a vibration pulse. Keep at least one method enabled at all times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Kill Switch Method Card ───────────────────────────────────────────────────

@Composable
fun KillSwitchMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    iconBackgroundColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    extraContent: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (enabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 0.8f else 0.4f
                        )
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

// ── Command Chip ──────────────────────────────────────────────────────────────

@Composable
fun CommandChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
