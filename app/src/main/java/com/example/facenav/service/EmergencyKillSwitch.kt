package com.example.facenav.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.facenav.R
import com.example.facenav.model.FacialGesture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Emergency Kill Switch for FaceNav
 *
 * isActive == true  → gesture detection is RUNNING
 * isActive == false → gesture detection is STOPPED
 *
 * Stop methods:
 *   1. Triple-blink  – blink any combination 3× within 2 s
 *   2. Voice command – "Stop FaceNav", "Emergency Stop", etc.
 *   3. Notification  – tap the "Emergency Stop" action in the shade
 *   4. Manual button – activate()/resume() called directly from UI
 */
class EmergencyKillSwitch(private val context: Context) {

    companion object {
        private const val TAG = "EmergencyKillSwitch"
        const val NOTIFICATION_ID = 999
        const val CHANNEL_ID = "EmergencyKillSwitchChannel"
        private const val TRIPLE_BLINK_WINDOW_MS = 2000L

        const val ACTION_EMERGENCY_STOP = "com.example.facenav.ACTION_EMERGENCY_STOP"
        const val ACTION_RESUME = "com.example.facenav.ACTION_RESUME"

        @Volatile
        private var instance: EmergencyKillSwitch? = null

        fun getInstance(): EmergencyKillSwitch? = instance

        fun getOrCreate(context: Context): EmergencyKillSwitch {
            return instance ?: synchronized(this) {
                instance ?: EmergencyKillSwitch(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // Triple-blink state
    private var blinkCount = 0
    private var firstBlinkTime = 0L

    // Feature flags (persist through updateSettings)
    private var tripleBlinkEnabled = true
    private var voiceCommandEnabled = true
    private var notificationEnabled = true



    // ─── Core API ──────────────────────────────────────────────────────────────

    /**
     * Call this from FaceDetectionService every time any gesture is emitted.
     * Counts blink-type gestures toward the triple-blink kill threshold.
     */
    fun onBlinkDetected(gesture: FacialGesture) {
        if (!tripleBlinkEnabled || !_isActive.value) return

        val isBlink = gesture == FacialGesture.SINGLE_BLINK ||
                gesture == FacialGesture.DOUBLE_BLINK ||
                gesture == FacialGesture.LEFT_BLINK ||
                gesture == FacialGesture.RIGHT_BLINK
        if (!isBlink) return

        val now = System.currentTimeMillis()
        if (now - firstBlinkTime > TRIPLE_BLINK_WINDOW_MS) {
            blinkCount = 0
            firstBlinkTime = now
        }
        blinkCount++
        Log.d(TAG, "Kill-switch blink #$blinkCount (${now - firstBlinkTime} ms elapsed)")

        if (blinkCount >= 3) {
            blinkCount = 0
            firstBlinkTime = 0L
            activate("Triple Blink")
        }
    }

    /**
     * Feed raw speech-recognition text here.
     * Returns true when the kill switch reacted (stop or resume).
     */
    fun checkVoiceCommand(text: String): Boolean {
        if (!voiceCommandEnabled) return false
        val sanitized = text.replace("\n", " ").replace("\r", " ").trim()
        val lower = sanitized.lowercase()

        val stopWords = listOf(
            "stop facenav", "emergency stop", "stop face nav",
            "halt facenav", "stop navigation", "disable facenav"
        )
        val resumeWords = listOf(
            "resume facenav", "start facenav", "enable facenav", "restart facenav"
        )

        return when {
            !_isActive.value && resumeWords.any { lower.contains(it) } -> { resume(); true }
            _isActive.value && stopWords.any { lower.contains(it) } -> { activate("Voice Command"); true }
            else -> false
        }
    }

    /**
     * Stop gesture detection immediately.
     */
    fun activate(reason: String) {
        if (!_isActive.value) { Log.d(TAG, "Already stopped"); return }
        Log.d(TAG, "Kill switch ACTIVATED – ${reason.replace("\n", " ").replace("\r", " ")}")
        _isActive.value = false

        vibrateDevice(longArrayOf(0, 150, 80, 150))   // double-pulse

        if (notificationEnabled) showActivatedNotification(reason)

        // Stop the foreground service
        context.stopService(
            Intent(context, FaceDetectionService::class.java).apply {
                action = FaceDetectionService.ACTION_STOP
            }
        )
    }

    /**
     * Resume gesture detection.
     */
    fun resume() {
        if (_isActive.value) { Log.d(TAG, "Already running"); return }
        Log.d(TAG, "Kill switch DEACTIVATED – resuming")
        _isActive.value = true
        blinkCount = 0
        firstBlinkTime = 0L

        vibrateDevice(longArrayOf(0, 80))   // single short pulse

        if (notificationEnabled) showQuickAccessNotification()

        // Restart the foreground service
        context.startForegroundService(
            Intent(context, FaceDetectionService::class.java).apply {
                action = FaceDetectionService.ACTION_START
            }
        )
    }

    // ─── Notifications ─────────────────────────────────────────────────────────

    /** Persistent notification while running – has an inline "Emergency Stop" button. */
    fun showQuickAccessNotification() {
        if (!notificationEnabled) return
        createNotificationChannel()

        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, KillSwitchReceiver::class.java).apply { action = ACTION_EMERGENCY_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm().notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("FaceNav Active")
                .setContentText("Gesture detection running – triple-blink or tap to stop")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(R.drawable.ic_launcher_foreground, "Emergency Stop", pi)
                .build()
        )
    }

    /** Dismissible notification shown after kill switch fires – has a "Resume" button. */
    private fun showActivatedNotification(reason: String) {
        val safeReason = reason.replace("\n", " ").replace("\r", " ")
        createNotificationChannel()

        val pi = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, KillSwitchReceiver::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm().notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("FaceNav Stopped")
                .setContentText("Stopped by: $safeReason – tap Resume to restart")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .addAction(R.drawable.ic_launcher_foreground, "Resume", pi)
                .build()
        )
    }

    fun hideNotification() = nm().cancel(NOTIFICATION_ID)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm().createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Emergency Kill Switch",
                    NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Emergency stop controls for FaceNav"
                    enableVibration(true)
                    enableLights(true)
                }
            )
        }
    }

    private fun nm() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ─── Settings ──────────────────────────────────────────────────────────────

    fun setTripleBlinkEnabled(enabled: Boolean) {
        tripleBlinkEnabled = enabled
        if (!enabled) { blinkCount = 0; firstBlinkTime = 0L }
    }

    fun setVoiceCommandEnabled(enabled: Boolean) { voiceCommandEnabled = enabled }

    fun setNotificationEnabled(enabled: Boolean) {
        notificationEnabled = enabled
        if (!enabled) hideNotification()
        else if (_isActive.value) showQuickAccessNotification()
    }

    fun getSettings() = KillSwitchSettings(tripleBlinkEnabled, voiceCommandEnabled, notificationEnabled)

    fun updateSettings(s: KillSwitchSettings) {
        setTripleBlinkEnabled(s.tripleBlinkEnabled)
        setVoiceCommandEnabled(s.voiceCommandEnabled)
        setNotificationEnabled(s.notificationEnabled)
    }

    // ─── Haptics ───────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun vibrateDevice(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                else
                    v.vibrate(pattern, -1)
            }
        } catch (e: Exception) { Log.w(TAG, "Vibration failed: ${e.message}") }
    }
}

data class KillSwitchSettings(
    val tripleBlinkEnabled: Boolean = true,
    val voiceCommandEnabled: Boolean = true,
    val notificationEnabled: Boolean = true
)

/**
 * Single source of truth for whether FaceDetectionService is running.
 * Written by the service itself; observed by HomeScreen and TestModeScreen
 * so their UI always reflects the real state (Issues 2 & 3).
 */
object ServiceStateHolder {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun setRunning(running: Boolean) { _isRunning.value = running }
}