package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.RadarStateRepository

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ACTION_START -> {
                RadarStateRepository.setDetecting(true)
            }
            ACTION_PAUSE -> {
                RadarStateRepository.setDetecting(false)
            }
            ACTION_RESET -> {
                RadarStateRepository.resetStreak()
            }
            ACTION_CROP -> {
                // Send intent to ScreenCaptureService to show crop overlay
                val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    this.action = ScreenCaptureService.ACTION_SHOW_CROP
                }
                context.startService(serviceIntent)
            }
            ACTION_STOP -> {
                val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    this.action = ScreenCaptureService.ACTION_STOP
                }
                context.stopService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESET = "com.example.ACTION_RESET"
        const val ACTION_CROP = "com.example.ACTION_CROP"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }
}
