package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CropRegion
import com.example.model.DetectionConfig

class RadarPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("radar_preferences", Context.MODE_PRIVATE)

    fun loadConfig(): DetectionConfig {
        val threshold = prefs.getFloat(KEY_THRESHOLD, 2.00f).toDouble()
        val targetStreak = prefs.getInt(KEY_TARGET_STREAK, 7)
        val checksPerSec = prefs.getInt(KEY_CHECKS_PER_SEC, 10)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val sound = prefs.getBoolean(KEY_SOUND, false)
        val left = prefs.getFloat(KEY_CROP_LEFT, CropRegion.DEFAULT_TOP_RIBBON.leftRatio)
        val top = prefs.getFloat(KEY_CROP_TOP, CropRegion.DEFAULT_TOP_RIBBON.topRatio)
        val right = prefs.getFloat(KEY_CROP_RIGHT, CropRegion.DEFAULT_TOP_RIBBON.rightRatio)
        val bottom = prefs.getFloat(KEY_CROP_BOTTOM, CropRegion.DEFAULT_TOP_RIBBON.bottomRatio)
        val debounce = prefs.getLong(KEY_DEBOUNCE, 1000L)

        return DetectionConfig(
            threshold = threshold,
            targetStreak = targetStreak,
            checksPerSecond = checksPerSec,
            vibrateOnAlert = vibrate,
            soundOnAlert = sound,
            cropRegion = CropRegion(left, top, right, bottom),
            minRoundDebounceMs = debounce
        )
    }

    fun saveConfig(config: DetectionConfig) {
        prefs.edit().apply {
            putFloat(KEY_THRESHOLD, config.threshold.toFloat())
            putInt(KEY_TARGET_STREAK, config.targetStreak)
            putInt(KEY_CHECKS_PER_SEC, config.checksPerSecond)
            putBoolean(KEY_VIBRATE, config.vibrateOnAlert)
            putBoolean(KEY_SOUND, config.soundOnAlert)
            putFloat(KEY_CROP_LEFT, config.cropRegion.leftRatio)
            putFloat(KEY_CROP_TOP, config.cropRegion.topRatio)
            putFloat(KEY_CROP_RIGHT, config.cropRegion.rightRatio)
            putFloat(KEY_CROP_BOTTOM, config.cropRegion.bottomRatio)
            putLong(KEY_DEBOUNCE, config.minRoundDebounceMs)
            apply()
        }
    }

    fun saveCropRegion(cropRegion: CropRegion) {
        prefs.edit().apply {
            putFloat(KEY_CROP_LEFT, cropRegion.leftRatio)
            putFloat(KEY_CROP_TOP, cropRegion.topRatio)
            putFloat(KEY_CROP_RIGHT, cropRegion.rightRatio)
            putFloat(KEY_CROP_BOTTOM, cropRegion.bottomRatio)
            apply()
        }
    }

    companion object {
        private const val KEY_THRESHOLD = "key_threshold"
        private const val KEY_TARGET_STREAK = "key_target_streak"
        private const val KEY_CHECKS_PER_SEC = "key_checks_per_sec"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_CROP_LEFT = "key_crop_left"
        private const val KEY_CROP_TOP = "key_crop_top"
        private const val KEY_CROP_RIGHT = "key_crop_right"
        private const val KEY_CROP_BOTTOM = "key_crop_bottom"
        private const val KEY_DEBOUNCE = "key_debounce"
    }
}
