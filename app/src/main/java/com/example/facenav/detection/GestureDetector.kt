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
    // Tracks when a single blink candidate was recorded so we can fire it
    // on the very next frame after the double-blink window expires,
    // regardless of whether a face is still detected.
    private var singleBlinkPendingTime = 0L

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
     *
     * Fix – Issue 6: individual eye blinks now require the OTHER eye to be
     *   genuinely above EYE_OPEN_THRESHOLD, and both individual-eye flags are
     *   cleared whenever both eyes close together so a normal blink never
     *   accidentally triggers LEFT_BLINK / RIGHT_BLINK.
     *
     * Fix – Issue 7: SINGLE_BLINK is now driven by singleBlinkPendingTime so
     *   it fires on the very next frame after the double-blink window expires,
     *   even if the face briefly disappears between frames.
     */
    private fun detectBlink(face: Face, currentTime: Long): FacialGesture? {
        val leftEyeOpen  = face.leftEyeOpenProbability  ?: return checkPendingSingleBlink(currentTime)
        val rightEyeOpen = face.rightEyeOpenProbability ?: return checkPendingSingleBlink(currentTime)

        val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2f

        // When both eyes close together, cancel any pending individual-eye flags
        // so a normal blink is never misread as LEFT_BLINK / RIGHT_BLINK.
        if (leftEyeOpen < EYE_CLOSED_THRESHOLD && rightEyeOpen < EYE_CLOSED_THRESHOLD) {
            wasLeftEyeClosed  = false
            wasRightEyeClosed = false
        }

        // Left eye blink: left closed AND right genuinely open (above threshold)
        if (leftEyeOpen < EYE_CLOSED_THRESHOLD && rightEyeOpen > EYE_OPEN_THRESHOLD && !wasLeftEyeClosed) {
            wasLeftEyeClosed = true
            return checkPendingSingleBlink(currentTime)
        }
        if (leftEyeOpen > EYE_OPEN_THRESHOLD && wasLeftEyeClosed) {
            wasLeftEyeClosed = false
            return FacialGesture.LEFT_BLINK
        }

        // Right eye blink: right closed AND left genuinely open (above threshold)
        if (rightEyeOpen < EYE_CLOSED_THRESHOLD && leftEyeOpen > EYE_OPEN_THRESHOLD && !wasRightEyeClosed) {
            wasRightEyeClosed = true
            return checkPendingSingleBlink(currentTime)
        }
        if (rightEyeOpen > EYE_OPEN_THRESHOLD && wasRightEyeClosed) {
            wasRightEyeClosed = false
            return FacialGesture.RIGHT_BLINK
        }

        // Both eyes closed – start tracking a both-eyes blink
        if (avgEyeOpen < EYE_CLOSED_THRESHOLD && !wasEyesClosed) {
            wasEyesClosed = true
            return checkPendingSingleBlink(currentTime)
        }

        // Both eyes opened again – blink completed
        if (avgEyeOpen > EYE_OPEN_THRESHOLD && wasEyesClosed) {
            wasEyesClosed = false

            val timeSinceLastBlink = currentTime - lastBlinkTime

            if (timeSinceLastBlink < settings.doubleBlinkWindowMs) {
                blinkCount++
                if (blinkCount >= 2) {
                    blinkCount = 0
                    lastBlinkTime = 0L
                    singleBlinkPendingTime = 0L
                    return FacialGesture.DOUBLE_BLINK
                }
            } else {
                blinkCount = 1
            }

            lastBlinkTime = currentTime
            singleBlinkPendingTime = currentTime   // arm the single-blink timer
            return null
        }

        return checkPendingSingleBlink(currentTime)
    }

    /**
     * Returns SINGLE_BLINK if a blink candidate is pending and the
     * double-blink window has now expired; otherwise returns null.
     * Resets all blink state after firing.
     */
    private fun checkPendingSingleBlink(currentTime: Long): FacialGesture? {
        if (singleBlinkPendingTime > 0L &&
            blinkCount == 1 &&
            currentTime - singleBlinkPendingTime > settings.doubleBlinkWindowMs
        ) {
            blinkCount = 0
            lastBlinkTime = 0L
            singleBlinkPendingTime = 0L
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

        // Detect smile – Issue 14 fix: return immediately after firing so
        // MOUTH_OPEN (which uses the inverse probability) can never fire on
        // the same frame.
        val smileThreshold = settings.smileThreshold
        if (smilingProbability > smileThreshold && !wasSmiling) {
            wasSmiling = true
            wasMouthOpen = false   // ensure mouth-open state is cleared
            return FacialGesture.SMILE
        }
        if (smilingProbability < smileThreshold - 0.1f && wasSmiling) {
            wasSmiling = false
        }

        // MOUTH_OPEN approximated as inverse of smiling probability.
        // Only evaluated when smile is NOT active (wasSmiling == false).
        val mouthOpenApprox = 1.0f - smilingProbability
        val mouthThreshold = settings.mouthOpenThreshold

        if (mouthOpenApprox > mouthThreshold && !wasMouthOpen && !wasSmiling) {
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
        singleBlinkPendingTime = 0L
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