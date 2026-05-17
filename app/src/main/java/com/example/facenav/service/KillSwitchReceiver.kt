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
        val sanitizedAction = intent.action?.replace("\n", " ")?.replace("\r", " ") ?: ""
        Log.d(TAG, "Received action: $sanitizedAction")

        val killSwitch = EmergencyKillSwitch.getOrCreate(context)

        when (intent.action) {
            EmergencyKillSwitch.ACTION_EMERGENCY_STOP -> {
                Log.d(TAG, "Emergency stop triggered from notification")
                killSwitch.activate("Notification Button")
            }

            EmergencyKillSwitch.ACTION_RESUME -> {
                Log.d(TAG, "Resume triggered from notification")
                killSwitch.resume()
            }
        }
    }
}