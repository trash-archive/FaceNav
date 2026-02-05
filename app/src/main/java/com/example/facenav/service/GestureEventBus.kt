package com.example.facenav.service

import com.example.facenav.model.FacialGesture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for broadcasting gesture detection events
 * Used for real-time updates in Test Mode
 */
object GestureEventBus {

    data class GestureEvent(
        val gesture: FacialGesture,
        val timestamp: Long = System.currentTimeMillis(),
        val confidence: Float = 1.0f
    )

    private val _gestureEvents = MutableSharedFlow<GestureEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents.asSharedFlow()

    /**
     * Emit a gesture detection event
     */
    suspend fun emitGesture(gesture: FacialGesture, confidence: Float = 1.0f) {
        _gestureEvents.emit(
            GestureEvent(
                gesture = gesture,
                timestamp = System.currentTimeMillis(),
                confidence = confidence
            )
        )
    }

    /**
     * Emit a gesture detection event (non-suspending)
     */
    fun emitGestureSync(gesture: FacialGesture, confidence: Float = 1.0f) {
        _gestureEvents.tryEmit(
            GestureEvent(
                gesture = gesture,
                timestamp = System.currentTimeMillis(),
                confidence = confidence
            )
        )
    }
}