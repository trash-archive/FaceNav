package com.example.facenav.model

/**
 * Represents the different facial gestures that can be detected
 */
enum class FacialGesture {
    SINGLE_BLINK,
    DOUBLE_BLINK,
    LEFT_BLINK,
    RIGHT_BLINK,
    NOD_UP,
    NOD_DOWN,
    TURN_LEFT,
    TURN_RIGHT,
    MOUTH_OPEN,
    SMILE;

    fun getDisplayName(): String {
        return when (this) {
            SINGLE_BLINK -> "Single Blink"
            DOUBLE_BLINK -> "Double Blink"
            LEFT_BLINK -> "Left Eye Blink"
            RIGHT_BLINK -> "Right Eye Blink"
            NOD_UP -> "Nod Up"
            NOD_DOWN -> "Nod Down"
            TURN_LEFT -> "Turn Left"
            TURN_RIGHT -> "Turn Right"
            MOUTH_OPEN -> "Mouth Open"
            SMILE -> "Smile"
        }
    }

    fun getDescription(): String {
        return when (this) {
            SINGLE_BLINK -> "Blink both eyes"
            DOUBLE_BLINK -> "Blink twice quickly"
            LEFT_BLINK -> "Blink left eye only"
            RIGHT_BLINK -> "Blink right eye only"
            NOD_UP -> "Tilt head upward"
            NOD_DOWN -> "Tilt head downward"
            TURN_LEFT -> "Turn head left"
            TURN_RIGHT -> "Turn head right"
            MOUTH_OPEN -> "Open your mouth"
            SMILE -> "Smile widely"
        }
    }

    fun getEmoji(): String {
        return when (this) {
            SINGLE_BLINK -> "😉"
            DOUBLE_BLINK -> "😉😉"
            LEFT_BLINK -> "🫲😉"
            RIGHT_BLINK -> "😉🫱"
            NOD_UP -> "⬆️"
            NOD_DOWN -> "⬇️"
            TURN_LEFT -> "⬅️"
            TURN_RIGHT -> "➡️"
            MOUTH_OPEN -> "😮"
            SMILE -> "😊"
        }
    }
}

/**
 * Represents the actions that can be performed when a gesture is detected
 */
enum class AccessibilityAction {
    NONE,
    TAP,
    DOUBLE_TAP,
    SCROLL_UP,
    SCROLL_DOWN,
    BACK,
    HOME,
    RECENT_APPS,
    SCREENSHOT,
    VOLUME_UP,
    VOLUME_DOWN,
    LOCK_SCREEN,
    NOTIFICATIONS,
    QUICK_SETTINGS;

    fun getDisplayName(): String {
        return when (this) {
            NONE -> "None"
            TAP -> "Tap"
            DOUBLE_TAP -> "Double Tap"
            SCROLL_UP -> "Scroll Up"
            SCROLL_DOWN -> "Scroll Down"
            BACK -> "Back"
            HOME -> "Home"
            RECENT_APPS -> "Recent Apps"
            SCREENSHOT -> "Screenshot"
            VOLUME_UP -> "Volume Up"
            VOLUME_DOWN -> "Volume Down"
            LOCK_SCREEN -> "Lock Screen"
            NOTIFICATIONS -> "Notifications"
            QUICK_SETTINGS -> "Quick Settings"
        }
    }
}

/**
 * Represents a mapping between a gesture and an action
 */
data class GestureMapping(
    val gesture: FacialGesture,
    val action: AccessibilityAction,
    val enabled: Boolean = true
)

/**
 * Sensitivity levels for gesture detection
 */
enum class SensitivityLevel {
    LOW,
    MEDIUM,
    HIGH;

    fun getThresholdMultiplier(): Float {
        return when (this) {
            LOW -> 1.5f      // Less sensitive, requires more pronounced movements
            MEDIUM -> 1.0f   // Default sensitivity
            HIGH -> 0.7f     // More sensitive, detects smaller movements
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            LOW -> "Low"
            MEDIUM -> "Medium"
            HIGH -> "High"
        }
    }
}

/**
 * Configuration settings for the app
 */
data class AppSettings(
    val sensitivity: SensitivityLevel = SensitivityLevel.MEDIUM,
    val cooldownMs: Long = 500L,
    val detectionEnabled: Boolean = true,
    val blinkDurationThreshold: Long = 150L,
    val doubleBlinkWindowMs: Long = 500L,
    val headAngleThreshold: Float = 15f,
    val nodReturnDelayMs: Long = 300L, // NEW: Delay before detecting opposite nod
    val mouthOpenThreshold: Float = 0.6f,
    val smileThreshold: Float = 0.7f,
    val hapticFeedback: Boolean = true,
    val soundFeedback: Boolean = false,
    val cameraPreview: Boolean = true
)

/**
 * Detected gesture event
 */
data class GestureDetectionEvent(
    val gesture: FacialGesture,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)