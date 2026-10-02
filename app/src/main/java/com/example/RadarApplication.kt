package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.RadarPreferences
import com.example.data.RadarStateRepository

class RadarApplication : Application() {

    companion object {
        const val CHANNEL_ID = "streak_radar_channel"
        const val ALERT_CHANNEL_ID = "streak_radar_alerts"
    }

    override fun onCreate() {
        super.onCreate()

        // Load saved preferences
        val prefs = RadarPreferences(this)
        RadarStateRepository.initialize(prefs.loadConfig())

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Foreground Service Channel (Low importance / silent ongoing status)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "StreakRadar Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of screen multiplier radar"
                setShowBadge(false)
            }

            // Streak Alert Channel (High importance, vibration and sound)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Streak Target Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts triggered when streak target is reached"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
            }

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertChannel)
        }
    }
}
