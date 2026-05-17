package com.example.facenav.data

import android.content.Context
import androidx.datastore.preferences.core.*
import com.example.facenav.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages app preferences using DataStore
 */
class PreferencesManager(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // Keys for gesture mappings
        private val GESTURE_SINGLE_BLINK = stringPreferencesKey("gesture_single_blink")
        private val GESTURE_DOUBLE_BLINK = stringPreferencesKey("gesture_double_blink")
        private val GESTURE_LEFT_BLINK = stringPreferencesKey("gesture_left_blink")
        private val GESTURE_RIGHT_BLINK = stringPreferencesKey("gesture_right_blink")
        private val GESTURE_NOD_UP = stringPreferencesKey("gesture_nod_up")
        private val GESTURE_NOD_DOWN = stringPreferencesKey("gesture_nod_down")
        private val GESTURE_TURN_LEFT = stringPreferencesKey("gesture_turn_left")
        private val GESTURE_TURN_RIGHT = stringPreferencesKey("gesture_turn_right")
        private val GESTURE_MOUTH_OPEN = stringPreferencesKey("gesture_mouth_open")
        private val GESTURE_SMILE = stringPreferencesKey("gesture_smile")

        // Keys for gesture enabled states
        private val ENABLED_SINGLE_BLINK = booleanPreferencesKey("enabled_single_blink")
        private val ENABLED_DOUBLE_BLINK = booleanPreferencesKey("enabled_double_blink")
        private val ENABLED_LEFT_BLINK = booleanPreferencesKey("enabled_left_blink")
        private val ENABLED_RIGHT_BLINK = booleanPreferencesKey("enabled_right_blink")
        private val ENABLED_NOD_UP = booleanPreferencesKey("enabled_nod_up")
        private val ENABLED_NOD_DOWN = booleanPreferencesKey("enabled_nod_down")
        private val ENABLED_TURN_LEFT = booleanPreferencesKey("enabled_turn_left")
        private val ENABLED_TURN_RIGHT = booleanPreferencesKey("enabled_turn_right")
        private val ENABLED_MOUTH_OPEN = booleanPreferencesKey("enabled_mouth_open")
        private val ENABLED_SMILE = booleanPreferencesKey("enabled_smile")

        // Keys for settings
        private val SENSITIVITY = stringPreferencesKey("sensitivity")
        private val COOLDOWN_MS = longPreferencesKey("cooldown_ms")
        private val DETECTION_ENABLED = booleanPreferencesKey("detection_enabled")
        private val BLINK_DURATION_THRESHOLD = longPreferencesKey("blink_duration_threshold")
        private val DOUBLE_BLINK_WINDOW = longPreferencesKey("double_blink_window")
        private val HEAD_ANGLE_THRESHOLD = floatPreferencesKey("head_angle_threshold")
        private val NOD_RETURN_DELAY = longPreferencesKey("nod_return_delay")
        private val MOUTH_OPEN_THRESHOLD = floatPreferencesKey("mouth_open_threshold")
        private val SMILE_THRESHOLD = floatPreferencesKey("smile_threshold")
        private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")
        private val CAMERA_PREVIEW = booleanPreferencesKey("camera_preview")
        private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

        // Default mappings
        private val DEFAULT_MAPPINGS = mapOf(
            FacialGesture.SINGLE_BLINK to AccessibilityAction.TAP,
            FacialGesture.DOUBLE_BLINK to AccessibilityAction.SCREENSHOT,
            FacialGesture.LEFT_BLINK to AccessibilityAction.BACK,
            FacialGesture.RIGHT_BLINK to AccessibilityAction.RECENT_APPS,
            FacialGesture.NOD_UP to AccessibilityAction.SCROLL_UP,
            FacialGesture.NOD_DOWN to AccessibilityAction.SCROLL_DOWN,
            FacialGesture.TURN_LEFT to AccessibilityAction.VOLUME_DOWN,
            FacialGesture.TURN_RIGHT to AccessibilityAction.VOLUME_UP,
            FacialGesture.MOUTH_OPEN to AccessibilityAction.NOTIFICATIONS,
            FacialGesture.SMILE to AccessibilityAction.HOME
        )
    }

    /**
     * Get gesture mapping for a specific gesture
     */
    fun getGestureMapping(gesture: FacialGesture): Flow<GestureMapping> {
        return dataStore.data.map { preferences ->
            val actionKey = getGestureKey(gesture)
            val enabledKey = getEnabledKey(gesture)

            val actionString = preferences[actionKey] ?: DEFAULT_MAPPINGS[gesture]?.name
            val action = actionString?.let {
                try {
                    AccessibilityAction.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    DEFAULT_MAPPINGS[gesture] ?: AccessibilityAction.NONE
                }
            } ?: AccessibilityAction.NONE

            val enabled = preferences[enabledKey] ?: true

            GestureMapping(gesture, action, enabled)
        }
    }

    /**
     * Get all gesture mappings
     */
    fun getAllGestureMappings(): Flow<List<GestureMapping>> {
        return dataStore.data.map { preferences ->
            FacialGesture.values().map { gesture ->
                val actionKey = getGestureKey(gesture)
                val enabledKey = getEnabledKey(gesture)

                val actionString = preferences[actionKey] ?: DEFAULT_MAPPINGS[gesture]?.name
                val action = actionString?.let {
                    try {
                        AccessibilityAction.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        DEFAULT_MAPPINGS[gesture] ?: AccessibilityAction.NONE
                    }
                } ?: AccessibilityAction.NONE

                val enabled = preferences[enabledKey] ?: true

                GestureMapping(gesture, action, enabled)
            }
        }
    }

    /**
     * Save gesture mapping
     */
    suspend fun saveGestureMapping(gestureMapping: GestureMapping) {
        dataStore.edit { preferences ->
            preferences[getGestureKey(gestureMapping.gesture)] = gestureMapping.action.name
            preferences[getEnabledKey(gestureMapping.gesture)] = gestureMapping.enabled
        }
    }

    /**
     * Get app settings
     */
    fun getSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences ->
            AppSettings(
                sensitivity = preferences[SENSITIVITY]?.let {
                    try {
                        SensitivityLevel.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        SensitivityLevel.MEDIUM
                    }
                } ?: SensitivityLevel.MEDIUM,
                cooldownMs = preferences[COOLDOWN_MS] ?: 500L,
                detectionEnabled = preferences[DETECTION_ENABLED] ?: true,
                blinkDurationThreshold = preferences[BLINK_DURATION_THRESHOLD] ?: 150L,
                doubleBlinkWindowMs = preferences[DOUBLE_BLINK_WINDOW] ?: 500L,
                headAngleThreshold = preferences[HEAD_ANGLE_THRESHOLD] ?: 15f,
                nodReturnDelayMs = preferences[NOD_RETURN_DELAY] ?: 300L,
                mouthOpenThreshold = preferences[MOUTH_OPEN_THRESHOLD] ?: 0.6f,
                smileThreshold = preferences[SMILE_THRESHOLD] ?: 0.7f,
                hapticFeedback = preferences[HAPTIC_FEEDBACK] ?: true,
                soundFeedback = preferences[SOUND_FEEDBACK] ?: false,
                cameraPreview = preferences[CAMERA_PREVIEW] ?: true
            )
        }
    }

    /**
     * Save app settings
     */
    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SENSITIVITY] = settings.sensitivity.name
            preferences[COOLDOWN_MS] = settings.cooldownMs
            preferences[DETECTION_ENABLED] = settings.detectionEnabled
            preferences[BLINK_DURATION_THRESHOLD] = settings.blinkDurationThreshold
            preferences[DOUBLE_BLINK_WINDOW] = settings.doubleBlinkWindowMs
            preferences[HEAD_ANGLE_THRESHOLD] = settings.headAngleThreshold
            preferences[NOD_RETURN_DELAY] = settings.nodReturnDelayMs
            preferences[MOUTH_OPEN_THRESHOLD] = settings.mouthOpenThreshold
            preferences[SMILE_THRESHOLD] = settings.smileThreshold
            preferences[HAPTIC_FEEDBACK] = settings.hapticFeedback
            preferences[SOUND_FEEDBACK] = settings.soundFeedback
            preferences[CAMERA_PREVIEW] = settings.cameraPreview
        }
    }

    /**
     * Reset all settings to default
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun getGestureKey(gesture: FacialGesture): Preferences.Key<String> {
        return when (gesture) {
            FacialGesture.SINGLE_BLINK -> GESTURE_SINGLE_BLINK
            FacialGesture.DOUBLE_BLINK -> GESTURE_DOUBLE_BLINK
            FacialGesture.LEFT_BLINK -> GESTURE_LEFT_BLINK
            FacialGesture.RIGHT_BLINK -> GESTURE_RIGHT_BLINK
            FacialGesture.NOD_UP -> GESTURE_NOD_UP
            FacialGesture.NOD_DOWN -> GESTURE_NOD_DOWN
            FacialGesture.TURN_LEFT -> GESTURE_TURN_LEFT
            FacialGesture.TURN_RIGHT -> GESTURE_TURN_RIGHT
            FacialGesture.MOUTH_OPEN -> GESTURE_MOUTH_OPEN
            FacialGesture.SMILE -> GESTURE_SMILE
        }
    }

    private fun getEnabledKey(gesture: FacialGesture): Preferences.Key<Boolean> {
        return when (gesture) {
            FacialGesture.SINGLE_BLINK -> ENABLED_SINGLE_BLINK
            FacialGesture.DOUBLE_BLINK -> ENABLED_DOUBLE_BLINK
            FacialGesture.LEFT_BLINK -> ENABLED_LEFT_BLINK
            FacialGesture.RIGHT_BLINK -> ENABLED_RIGHT_BLINK
            FacialGesture.NOD_UP -> ENABLED_NOD_UP
            FacialGesture.NOD_DOWN -> ENABLED_NOD_DOWN
            FacialGesture.TURN_LEFT -> ENABLED_TURN_LEFT
            FacialGesture.TURN_RIGHT -> ENABLED_TURN_RIGHT
            FacialGesture.MOUTH_OPEN -> ENABLED_MOUTH_OPEN
            FacialGesture.SMILE -> ENABLED_SMILE
        }
    }

    fun hasSeenOnboarding(): Flow<Boolean> =
        dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }

    suspend fun setOnboardingSeen() {
        dataStore.edit { it[HAS_SEEN_ONBOARDING] = true }
    }
}