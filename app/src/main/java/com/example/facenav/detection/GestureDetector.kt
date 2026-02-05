package com.example.facenav.detection

import com.google.mlkit.vision.face.Face
import com.example.facenav.model.AppSettings
import com.example.facenav.model.FacialGesture
import kotlin.math.abs

/**
 * Detects facial gestures from ML Kit Face data with enhanced accuracy
 */
class GestureDetector(private var settings: AppSettings) {

    // State tracking for blinks
    private var lastBlinkTime = 0L
    private var blinkCount = 0
    private var wasEyesClosed = false
    private var wasLeftEyeClosed = false
    private var wasRightEyeClosed = false

    // State tracking for head movements with nod protection
    private var lastHeadEulerY = 0f
    private var lastHeadEulerX = 0f
    private var lastNodDirection: NodDirection? = null
    private var lastNodTime = 0L

    // State tracking for mouth gestures
    private var wasMouthOpen = false
    private var wasSmiling = false

    // Cooldown tracking
    private val lastGestureTime = mutableMapOf<FacialGesture, Long>()

    companion object {
        private const val EYE_CLOSED_THRESHOLD = 0.3f
        private const val EYE_OPEN_THRESHOLD = 0.6f
        private const val MOUTH_OPEN_THRESHOLD_DEFAULT = 0.6f
        private const val SMILE_THRESHOLD_DEFAULT = 0.7f
    }

    private enum class NodDirection {
        UP, DOWN
    }

    /**
     * Update settings for gesture detection
     */
    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
    }

    /**
     * Process a face and detect gestures
     * Returns null if no gesture detected or gesture is in cooldown
     */
    fun detectGesture(face: Face): FacialGesture? {
        val currentTime = System.currentTimeMillis()

        // Check for mouth gestures first (they're independent)
        val mouthGesture = detectMouthGesture(face, currentTime)
        if (mouthGesture != null && !isInCooldown(mouthGesture, currentTime)) {
            recordGesture(mouthGesture, currentTime)
            return mouthGesture
        }

        // Check for blinks (including individual eye blinks)
        val blinkGesture = detectBlink(face, currentTime)
        if (blinkGesture != null && !isInCooldown(blinkGesture, currentTime)) {
            recordGesture(blinkGesture, currentTime)
            return blinkGesture
        }

        // Check for head movements with nod delay protection
        val headGesture = detectHeadMovement(face, currentTime)
        if (headGesture != null && !isInCooldown(headGesture, currentTime)) {
            recordGesture(headGesture, currentTime)
            return headGesture
        }

        return null
    }

    /**
     * Detect blink gestures (single, double, left, right)
     */
    private fun detectBlink(face: Face, currentTime: Long): FacialGesture? {
        val leftEyeOpen = face.leftEyeOpenProbability ?: return null
        val rightEyeOpen = face.rightEyeOpenProbability ?: return null

        val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2f

        // Detect individual eye blinks
        // Left eye blink (right eye stays open)
        if (leftEyeOpen < EYE_CLOSED_THRESHOLD && rightEyeOpen > EYE_OPEN_THRESHOLD && !wasLeftEyeClosed) {
            wasLeftEyeClosed = true
            return null
        }
        if (leftEyeOpen > EYE_OPEN_THRESHOLD && wasLeftEyeClosed) {
            wasLeftEyeClosed = false
            return FacialGesture.LEFT_BLINK
        }

        // Right eye blink (left eye stays open)
        if (rightEyeOpen < EYE_CLOSED_THRESHOLD && leftEyeOpen > EYE_OPEN_THRESHOLD && !wasRightEyeClosed) {
            wasRightEyeClosed = true
            return null
        }
        if (rightEyeOpen > EYE_OPEN_THRESHOLD && wasRightEyeClosed) {
            wasRightEyeClosed = false
            return FacialGesture.RIGHT_BLINK
        }

        // Detect both eyes closure (for single/double blink)
        if (avgEyeOpen < EYE_CLOSED_THRESHOLD && !wasEyesClosed) {
            wasEyesClosed = true
            return null
        }

        // Detect both eyes opening (blink completed)
        if (avgEyeOpen > EYE_OPEN_THRESHOLD && wasEyesClosed) {
            wasEyesClosed = false

            val timeSinceLastBlink = currentTime - lastBlinkTime

            // Check if this could be a double blink
            if (timeSinceLastBlink < settings.doubleBlinkWindowMs) {
                blinkCount++
                if (blinkCount >= 2) {
                    blinkCount = 0
                    lastBlinkTime = 0L
                    return FacialGesture.DOUBLE_BLINK
                }
            } else {
                // Too long since last blink, reset count
                blinkCount = 1
            }

            lastBlinkTime = currentTime
            return null
        }

        // Check if single blink window has passed
        if (blinkCount == 1 && currentTime - lastBlinkTime > settings.doubleBlinkWindowMs) {
            blinkCount = 0
            return FacialGesture.SINGLE_BLINK
        }

        return null
    }

    /**
     * Detect head movement gestures with nod delay protection
     */
    private fun detectHeadMovement(face: Face, currentTime: Long): FacialGesture? {
        val eulerY = face.headEulerAngleY // Horizontal rotation (left/right)
        val eulerX = face.headEulerAngleX // Vertical rotation (up/down)

        val threshold = settings.headAngleThreshold * settings.sensitivity.getThresholdMultiplier()

        // Detect horizontal head turn (left/right)
        val horizontalDelta = eulerY - lastHeadEulerY
        if (abs(horizontalDelta) > threshold) {
            lastHeadEulerY = eulerY
            return when {
                horizontalDelta > 0 -> FacialGesture.TURN_RIGHT
                horizontalDelta < 0 -> FacialGesture.TURN_LEFT
                else -> null
            }
        }

        // Detect vertical head nod (up/down) with delay protection
        val verticalDelta = eulerX - lastHeadEulerX
        if (abs(verticalDelta) > threshold) {
            val currentNodDirection = when {
                verticalDelta > 0 -> NodDirection.DOWN
                verticalDelta < 0 -> NodDirection.UP
                else -> null
            }

            // Check if this is a return to center (opposite of last nod)
            if (currentNodDirection != null && lastNodDirection != null) {
                val isReturnToCenter = (currentNodDirection == NodDirection.UP && lastNodDirection == NodDirection.DOWN) ||
                        (currentNodDirection == NodDirection.DOWN && lastNodDirection == NodDirection.UP)

                val timeSinceLastNod = currentTime - lastNodTime

                // If this looks like a return to center and it's within the delay window, ignore it
                if (isReturnToCenter && timeSinceLastNod < settings.nodReturnDelayMs) {
                    lastHeadEulerX = eulerX
                    return null
                }
            }

            // Record this nod
            lastHeadEulerX = eulerX
            lastNodDirection = currentNodDirection
            lastNodTime = currentTime

            return when (currentNodDirection) {
                NodDirection.DOWN -> FacialGesture.NOD_DOWN
                NodDirection.UP -> FacialGesture.NOD_UP
                null -> null
            }
        }

        // Update baseline values slowly to adjust to user's position
        lastHeadEulerY = eulerY * 0.1f + lastHeadEulerY * 0.9f
        lastHeadEulerX = eulerX * 0.1f + lastHeadEulerX * 0.9f

        return null
    }

    /**
     * Detect mouth gestures (open mouth, smile)
     */
    private fun detectMouthGesture(face: Face, currentTime: Long): FacialGesture? {
        // ML Kit doesn't directly provide mouth open probability
        // We'll use smiling probability as an approximation for smile
        // For mouth open, we'd need custom model or use face contours

        val smilingProbability = face.smilingProbability ?: return null

        // Detect smile
        val smileThreshold = settings.smileThreshold
        if (smilingProbability > smileThreshold && !wasSmiling) {
            wasSmiling = true
            return FacialGesture.SMILE
        }
        if (smilingProbability < smileThreshold - 0.1f && wasSmiling) {
            wasSmiling = false
        }

        // Note: Mouth open detection would require additional processing
        // of face contours or a custom ML model. For now, we'll detect it
        // based on face height changes or use smiling as inverse
        val mouthOpenApprox = 1.0f - smilingProbability
        val mouthThreshold = settings.mouthOpenThreshold

        if (mouthOpenApprox > mouthThreshold && !wasMouthOpen) {
            wasMouthOpen = true
            return FacialGesture.MOUTH_OPEN
        }
        if (mouthOpenApprox < mouthThreshold - 0.1f && wasMouthOpen) {
            wasMouthOpen = false
        }

        return null
    }

    /**
     * Check if gesture is in cooldown period
     */
    private fun isInCooldown(gesture: FacialGesture, currentTime: Long): Boolean {
        val lastTime = lastGestureTime[gesture] ?: return false
        return currentTime - lastTime < settings.cooldownMs
    }

    /**
     * Record that a gesture was detected
     */
    private fun recordGesture(gesture: FacialGesture, currentTime: Long) {
        lastGestureTime[gesture] = currentTime
    }

    /**
     * Reset all gesture state
     */
    fun reset() {
        lastBlinkTime = 0L
        blinkCount = 0
        wasEyesClosed = false
        wasLeftEyeClosed = false
        wasRightEyeClosed = false
        lastHeadEulerY = 0f
        lastHeadEulerX = 0f
        lastNodDirection = null
        lastNodTime = 0L
        wasMouthOpen = false
        wasSmiling = false
        lastGestureTime.clear()
    }
}