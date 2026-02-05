package com.example.facenav.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver for Emergency Kill Switch notification actions
 */
class KillSwitchReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KillSwitchReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            EmergencyKillSwitch.ACTION_EMERGENCY_STOP -> {
                Log.d(TAG, "Emergency stop triggered from notification")

                // Stop the face detection service
                val stopIntent = Intent(context, FaceDetectionService::class.java).apply {
                    action = FaceDetectionService.ACTION_STOP
                }
                context.stopService(stopIntent)
            }

            EmergencyKillSwitch.ACTION_RESUME -> {
                Log.d(TAG, "Resume triggered from notification")

                // Restart the face detection service
                val startIntent = Intent(context, FaceDetectionService::class.java).apply {
                    action = FaceDetectionService.ACTION_START
                }
                context.startForegroundService(startIntent)
            }
        }
    }
}