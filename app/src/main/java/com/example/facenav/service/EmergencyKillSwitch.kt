package com.example.facenav.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.facenav.R
import com.example.facenav.model.FacialGesture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Emergency Kill Switch for FaceNav
 * Provides multiple ways to stop gesture detection:
 * - Triple blink detection
 * - Voice commands
 * - Notification button
 */
class EmergencyKillSwitch(private val context: Context) {

    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // Triple blink tracking
    private var blinkCount = 0
    private var firstBlinkTime = 0L

    // Voice command tracking
    private var voiceCommandEnabled = true

    companion object {
        private const val TAG = "EmergencyKillSwitch"
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "EmergencyKillSwitchChannel"
        private const val TRIPLE_BLINK_WINDOW_MS = 2000L // 2 seconds

        const val ACTION_EMERGENCY_STOP = "ACTION_EMERGENCY_STOP"
        const val ACTION_RESUME = "ACTION_RESUME"
    }

    /**
     * Check if a detected gesture is a blink and track for triple blink
     */
    fun onBlinkDetected(gesture: FacialGesture) {
        // Only track blinks for triple blink detection
        if (gesture != FacialGesture.SINGLE_BLINK &&
            gesture != FacialGesture.DOUBLE_BLINK) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // Reset count if too much time has passed
        if (currentTime - firstBlinkTime > TRIPLE_BLINK_WINDOW_MS) {
            blinkCount = 0
            firstBlinkTime = currentTime
        }

        blinkCount++
        Log.d(TAG, "Blink detected: count=$blinkCount, time=${currentTime - firstBlinkTime}ms")

        // Triple blink detected!
        if (blinkCount >= 3) {
            Log.d(TAG, "Triple blink detected! Activating kill switch")
            activate("Triple Blink")
            blinkCount = 0
        }
    }

    /**
     * Check voice command input
     * Returns true if kill switch was triggered
     */
    fun checkVoiceCommand(text: String): Boolean {
        if (!voiceCommandEnabled || !_isActive.value) return false

        val lowerText = text.lowercase()

        // Standard commands
        val standardCommands = listOf(
            "stop facenav",
            "emergency stop",
            "stop face nav",
            "halt facenav"
        )

        // Fun commands 😏
        val funCommands = listOf(
            "moan",
            "mmmm",
            "ohhh",
            "ahhhh"
        )

        return when {
            standardCommands.any { lowerText.contains(it) } -> {
                activate("Voice Command")
                true
            }
            funCommands.any { lowerText.contains(it) } -> {
                activate("Fun Voice Command 😏")
                true
            }
            else -> false
        }
    }

    /**
     * Activate the kill switch (stop gesture detection)
     */
    fun activate(reason: String) {
        if (!_isActive.value) {
            Log.d(TAG, "Kill switch already active")
            return
        }

        Log.d(TAG, "Kill switch activated: $reason")
        _isActive.value = false

        // Update notification
        showActivatedNotification(reason)

        // Broadcast stop event
        val intent = Intent(context, FaceDetectionService::class.java).apply {
            action = FaceDetectionService.ACTION_STOP
        }
        context.stopService(intent)
    }

    /**
     * Resume gesture detection
     */
    fun resume() {
        if (_isActive.value) {
            Log.d(TAG, "Kill switch already inactive")
            return
        }

        Log.d(TAG, "Kill switch deactivated - resuming")
        _isActive.value = true
        blinkCount = 0

        // Update notification
        showQuickAccessNotification()
    }

    /**
     * Show quick access notification when service is running
     */
    fun showQuickAccessNotification() {
        createNotificationChannel()

        // Create intent for emergency stop
        val stopIntent = Intent(context, KillSwitchReceiver::class.java).apply {
            action = ACTION_EMERGENCY_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Emergency Stop Available")
            .setContentText("Tap to stop FaceNav immediately")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Emergency Stop",
                stopPendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show notification when kill switch is activated
     */
    private fun showActivatedNotification(reason: String) {
        createNotificationChannel()

        // Create intent for resume
        val resumeIntent = Intent(context, KillSwitchReceiver::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("FaceNav Stopped")
            .setContentText("Stopped by: $reason")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Resume",
                resumePendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Hide the kill switch notification
     */
    fun hideNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Kill Switch",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency stop button for FaceNav"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Enable/disable voice commands
     */
    fun setVoiceCommandEnabled(enabled: Boolean) {
        voiceCommandEnabled = enabled
        Log.d(TAG, "Voice commands ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get current settings
     */
    fun getSettings(): KillSwitchSettings {
        return KillSwitchSettings(
            tripleBlinkEnabled = true,
            voiceCommandEnabled = voiceCommandEnabled,
            notificationEnabled = true
        )
    }

    /**
     * Update settings
     */
    fun updateSettings(settings: KillSwitchSettings) {
        voiceCommandEnabled = settings.voiceCommandEnabled
        Log.d(TAG, "Settings updated: $settings")
    }
}

/**
 * Kill switch settings
 */
data class KillSwitchSettings(
    val tripleBlinkEnabled: Boolean = true,
    val voiceCommandEnabled: Boolean = true,
    val notificationEnabled: Boolean = true
)