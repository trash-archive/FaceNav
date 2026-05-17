package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.facenav.data.PreferencesManager
import com.example.facenav.model.AccessibilityAction
import com.example.facenav.model.FacialGesture
import com.example.facenav.model.GestureMapping
import kotlinx.coroutines.launch

// ── Gesture icon mapping ──────────────────────────────────────────────────────

fun FacialGesture.toMaterialIcon(): ImageVector = when (this) {
    FacialGesture.SINGLE_BLINK  -> Icons.Outlined.RemoveRedEye
    FacialGesture.DOUBLE_BLINK  -> Icons.Outlined.Visibility
    FacialGesture.LEFT_BLINK    -> Icons.Outlined.KeyboardArrowLeft
    FacialGesture.RIGHT_BLINK   -> Icons.Outlined.KeyboardArrowRight
    FacialGesture.NOD_UP        -> Icons.Outlined.KeyboardArrowUp
    FacialGesture.NOD_DOWN      -> Icons.Outlined.KeyboardArrowDown
    FacialGesture.TURN_LEFT     -> Icons.Outlined.ArrowBack
    FacialGesture.TURN_RIGHT    -> Icons.Outlined.ArrowForward
    FacialGesture.MOUTH_OPEN    -> Icons.Outlined.ChatBubbleOutline
    FacialGesture.SMILE         -> Icons.Outlined.Mood
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMappingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val gestureMappings by preferencesManager.getAllGestureMappings().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestures", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Map gestures to actions",
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
        if (gestureMappings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Info banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Toggle gestures on/off and assign an action to each one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                items(gestureMappings) { mapping ->
                    GestureMappingCard(
                        mapping = mapping,
                        onMappingChanged = { updated ->
                            scope.launch { preferencesManager.saveGestureMapping(updated) }
                        }
                    )
                }
            }
        }
    }
}

// ── Mapping Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMappingCard(
    mapping: GestureMapping,
    onMappingChanged: (GestureMapping) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(mapping.action) }
    var isEnabled by remember { mutableStateOf(mapping.enabled) }

    LaunchedEffect(mapping) {
        selectedAction = mapping.action
        isEnabled = mapping.enabled
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rotation"
    )

    val (iconVector, iconBg, iconTint) = gestureIconStyle(mapping.gesture, isEnabled)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = if (isEnabled) 1.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = iconBg
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = iconTint
                        )
                    }
                    Column {
                        Text(
                            mapping.gesture.getDisplayName(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            mapping.gesture.getDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 0.8f else 0.4f
                            )
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { on ->
                        isEnabled = on
                        onMappingChanged(mapping.copy(enabled = on))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Action dropdown
            AnimatedVisibility(
                visible = isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = if (expanded)
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = selectedAction.toIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    selectedAction.getDisplayName(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).rotate(rotationAngle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AccessibilityAction.values().forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            action.toIcon(),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (action == selectedAction)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            action.getDisplayName(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            color = if (action == selectedAction)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (action == selectedAction) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedAction = action
                                    expanded = false
                                    onMappingChanged(mapping.copy(action = action))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun gestureIconStyle(gesture: FacialGesture, enabled: Boolean): Triple<ImageVector, Color, Color> {
    val disabledBg   = MaterialTheme.colorScheme.surfaceVariant
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    val (bg, tint) = when (gesture) {
        FacialGesture.SINGLE_BLINK,
        FacialGesture.DOUBLE_BLINK ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        FacialGesture.LEFT_BLINK,
        FacialGesture.RIGHT_BLINK ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        FacialGesture.NOD_UP,
        FacialGesture.NOD_DOWN ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        FacialGesture.TURN_LEFT,
        FacialGesture.TURN_RIGHT ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        FacialGesture.MOUTH_OPEN ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        FacialGesture.SMILE ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

    return Triple(
        gesture.toMaterialIcon(),
        if (enabled) bg else disabledBg,
        if (enabled) tint else disabledTint
    )
}

fun AccessibilityAction.toIcon(): ImageVector = when (this) {
    AccessibilityAction.NONE           -> Icons.Outlined.Block
    AccessibilityAction.TAP            -> Icons.Outlined.TouchApp
    AccessibilityAction.DOUBLE_TAP     -> Icons.Outlined.AdsClick
    AccessibilityAction.SCROLL_UP      -> Icons.Outlined.KeyboardArrowUp
    AccessibilityAction.SCROLL_DOWN    -> Icons.Outlined.KeyboardArrowDown
    AccessibilityAction.BACK           -> Icons.Outlined.ArrowBack
    AccessibilityAction.HOME           -> Icons.Outlined.Home
    AccessibilityAction.RECENT_APPS    -> Icons.Outlined.Apps
    AccessibilityAction.SCREENSHOT     -> Icons.Outlined.Screenshot
    AccessibilityAction.VOLUME_UP      -> Icons.Outlined.VolumeUp
    AccessibilityAction.VOLUME_DOWN    -> Icons.Outlined.VolumeDown
    AccessibilityAction.LOCK_SCREEN    -> Icons.Outlined.Lock
    AccessibilityAction.NOTIFICATIONS  -> Icons.Outlined.Notifications
    AccessibilityAction.QUICK_SETTINGS -> Icons.Outlined.Settings
}
