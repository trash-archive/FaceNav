package com.example.facenav.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.facenav.model.AccessibilityAction

/**
 * Accessibility Service that performs actions based on facial gestures
 */
class FaceNavAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FaceNavAccessService"
        // WeakReference prevents a stale service instance from leaking the
        // entire service context if the system restarts the service (Issue 9).
        private var instanceRef = java.lang.ref.WeakReference<FaceNavAccessibilityService>(null)

        fun getInstance(): FaceNavAccessibilityService? = instanceRef.get()

        fun isServiceEnabled(): Boolean = instanceRef.get() != null
    }

    override fun onCreate() {
        super.onCreate()
        instanceRef = java.lang.ref.WeakReference(this)
        Log.d(TAG, "FaceNav Accessibility Service Created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef.clear()
        Log.d(TAG, "FaceNav Accessibility Service Destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for our use case
        // This service is mainly for performing actions
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "FaceNav Accessibility Service Connected")
    }

    /**
     * Perform an accessibility action
     */
    fun performAction(action: AccessibilityAction): Boolean {
        if (!isServiceEnabled()) return false
        Log.d(TAG, "Performing action: ${action.name.replace("\n", " ").replace("\r", " ")}")

        return try {
            when (action) {
                AccessibilityAction.NONE -> true
                AccessibilityAction.TAP -> performTap()
                AccessibilityAction.DOUBLE_TAP -> performDoubleTap()
                AccessibilityAction.SCROLL_UP -> performScroll(true)
                AccessibilityAction.SCROLL_DOWN -> performScroll(false)
                AccessibilityAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                AccessibilityAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                AccessibilityAction.RECENT_APPS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                AccessibilityAction.SCREENSHOT -> performScreenshot()
                AccessibilityAction.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                AccessibilityAction.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                AccessibilityAction.LOCK_SCREEN -> performLockScreen()
                AccessibilityAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                AccessibilityAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing action: ${action.name}", e)
            false
        }
    }

    /**
     * Perform a tap gesture at the center of the screen
     */
    private fun performTap(): Boolean {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        val path = Path()
        path.moveTo(centerX, centerY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))

        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Perform a double tap gesture
     */
    private fun performDoubleTap(): Boolean {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        val path = Path()
        path.moveTo(centerX, centerY)

        val gestureBuilder = GestureDescription.Builder()

        // First tap
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))

        // Second tap (100ms after first)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 100, 50))

        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Perform a scroll gesture
     */
    private fun performScroll(up: Boolean): Boolean {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = if (up) displayMetrics.heightPixels * 0.7f else displayMetrics.heightPixels * 0.3f
        val endY = if (up) displayMetrics.heightPixels * 0.3f else displayMetrics.heightPixels * 0.7f

        val path = Path()
        path.moveTo(centerX, startY)
        path.lineTo(centerX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 200))

        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Take a screenshot
     */
    private fun performScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            Log.w(TAG, "Screenshot action not available on this Android version")
            false
        }
    }

    /**
     * Lock the screen
     */
    private fun performLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Log.w(TAG, "Lock screen action not available on this Android version")
            false
        }
    }

    /**
     * Adjust volume
     */
    private fun adjustVolume(direction: Int): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
        return true
    }
}