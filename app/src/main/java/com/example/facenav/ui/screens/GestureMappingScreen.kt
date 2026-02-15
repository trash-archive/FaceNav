package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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

// ─── Icon + tint mapping for every gesture ────────────────────────────────────

/**
 * Returns the Material icon that best represents this gesture.
 * No emojis – every icon comes from Icons.Default.
 */
fun FacialGesture.toMaterialIcon(): ImageVector = when (this) {
    FacialGesture.SINGLE_BLINK  -> Icons.Default.RemoveRedEye
    FacialGesture.DOUBLE_BLINK  -> Icons.Default.Visibility
    FacialGesture.LEFT_BLINK    -> Icons.Default.KeyboardArrowLeft
    FacialGesture.RIGHT_BLINK   -> Icons.Default.KeyboardArrowRight
    FacialGesture.NOD_UP        -> Icons.Default.KeyboardArrowUp
    FacialGesture.NOD_DOWN      -> Icons.Default.KeyboardArrowDown
    FacialGesture.TURN_LEFT     -> Icons.Default.ArrowBack
    FacialGesture.TURN_RIGHT    -> Icons.Default.ArrowForward
    FacialGesture.MOUTH_OPEN    -> Icons.Default.ChatBubbleOutline
    FacialGesture.SMILE         -> Icons.Default.Mood
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMappingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    val gestureMappings by preferencesManager.getAllGestureMappings()
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gesture Mapping")
                        Text(
                            "Customize gesture actions",
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
        if (gestureMappings.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Configure Actions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Map each facial gesture to a specific phone action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
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

// ─── Mapping Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMappingCard(
    mapping: GestureMapping,
    onMappingChanged: (GestureMapping) -> Unit
) {
    var expanded      by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(mapping.action) }
    var isEnabled     by remember { mutableStateOf(mapping.enabled) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )

    // Icon + background colour for each gesture
    val (iconVector, iconBg, iconTint) = gestureIconStyle(mapping.gesture, isEnabled)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEnabled) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header Row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Gesture icon (Material icon, not emoji)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(iconBg, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = mapping.gesture.getDisplayName(),
                            modifier = Modifier.size(26.dp),
                            tint = iconTint
                        )
                    }

                    Column {
                        Text(
                            text = mapping.gesture.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Text(
                            text = mapping.gesture.getDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isEnabled) 0.8f else 0.45f
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

            // ── Action Dropdown (visible when enabled) ────────────────────────
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (expanded)
                            androidx.compose.foundation.BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            )
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Action",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
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
                                        text = selectedAction.getDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(rotationAngle),
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
                                            imageVector = action.toIcon(),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (action == selectedAction)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            action.getDisplayName(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (action == selectedAction) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedAction = action
                                    expanded = false
                                    onMappingChanged(mapping.copy(action = action))
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (action == selectedAction)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Returns (icon, backgroundColour, tintColour) for the gesture icon box.
 * Each gesture group gets a distinctive colour token so they're easy to scan.
 */
@Composable
private fun gestureIconStyle(gesture: FacialGesture, enabled: Boolean): Triple<ImageVector, Color, Color> {
    val disabledBg   = MaterialTheme.colorScheme.surfaceVariant
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    val (bg, tint) = when (gesture) {
        FacialGesture.SINGLE_BLINK,
        FacialGesture.DOUBLE_BLINK  ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        FacialGesture.LEFT_BLINK,
        FacialGesture.RIGHT_BLINK   ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        FacialGesture.NOD_UP,
        FacialGesture.NOD_DOWN      ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        FacialGesture.TURN_LEFT,
        FacialGesture.TURN_RIGHT    ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        FacialGesture.MOUTH_OPEN    ->
            MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.5f) to
                    MaterialTheme.colorScheme.onSurface
        FacialGesture.SMILE         ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

    return Triple(
        gesture.toMaterialIcon(),
        if (enabled) bg else disabledBg,
        if (enabled) tint else disabledTint
    )
}

/** Material icon for each AccessibilityAction – shown in the dropdown. */
fun AccessibilityAction.toIcon(): ImageVector = when (this) {
    AccessibilityAction.NONE          -> Icons.Default.Block
    AccessibilityAction.TAP           -> Icons.Default.TouchApp
    AccessibilityAction.DOUBLE_TAP    -> Icons.Default.AdsClick
    AccessibilityAction.SCROLL_UP     -> Icons.Default.KeyboardArrowUp
    AccessibilityAction.SCROLL_DOWN   -> Icons.Default.KeyboardArrowDown
    AccessibilityAction.BACK          -> Icons.Default.ArrowBack
    AccessibilityAction.HOME          -> Icons.Default.Home
    AccessibilityAction.RECENT_APPS   -> Icons.Default.Apps
    AccessibilityAction.SCREENSHOT    -> Icons.Default.Screenshot
    AccessibilityAction.VOLUME_UP     -> Icons.Default.VolumeUp
    AccessibilityAction.VOLUME_DOWN   -> Icons.Default.VolumeDown
    AccessibilityAction.LOCK_SCREEN   -> Icons.Default.Lock
    AccessibilityAction.NOTIFICATIONS -> Icons.Default.Notifications
    AccessibilityAction.QUICK_SETTINGS -> Icons.Default.Settings
}