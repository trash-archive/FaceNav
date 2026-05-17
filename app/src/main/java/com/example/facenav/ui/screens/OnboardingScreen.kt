package com.example.facenav.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.facenav.R
import com.example.facenav.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val features = listOf(
        FeatureItem(Icons.Outlined.RemoveRedEye, "Blink to Tap",       "Control with eye blinks"),
        FeatureItem(Icons.Outlined.SwapVert,     "Nod to Scroll",      "Natural head movements"),
        FeatureItem(Icons.Outlined.Sync,         "Turn to Navigate",   "Look left or right"),
        FeatureItem(Icons.Outlined.Tune,         "Fully Customizable", "Map any gesture to any action")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // ── App icon badge ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Image(
                painter = painterResource(id = R.drawable.facenav_logo_ver2),
                contentDescription = "FaceNav",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Title ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 200)) +
                    slideInVertically(tween(600, delayMillis = 200)) { 30 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Welcome to",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
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
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Subtitle ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 400)) +
                    slideInVertically(tween(600, delayMillis = 400)) { 30 }
        ) {
            Text(
                "Navigate your phone hands-free\nwith facial gestures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── Feature cards ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEachIndexed { index, feature ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 600 + index * 100)) +
                            slideInHorizontally(tween(500, delayMillis = 600 + index * 100)) { -40 }
                ) {
                    FeatureCard(feature)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── CTA button ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 1100)) +
                    slideInVertically(tween(600, delayMillis = 1100)) { 40 }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        scope.launch { preferencesManager.setOnboardingSeen() }
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "Get Started",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    "You can change all settings later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Feature card ──────────────────────────────────────────────────────────────

data class FeatureItem(val icon: ImageVector, val title: String, val description: String)

@Composable
fun FeatureCard(feature: FeatureItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
