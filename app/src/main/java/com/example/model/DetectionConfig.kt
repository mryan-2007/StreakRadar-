package com.example.model

data class DetectionConfig(
    val threshold: Double = 2.00,
    val targetStreak: Int = 7,
    val checksPerSecond: Int = 10,
    val vibrateOnAlert: Boolean = true,
    val soundOnAlert: Boolean = false,
    val cropRegion: CropRegion = CropRegion.DEFAULT_TOP_RIBBON,
    val minRoundDebounceMs: Long = 1000L
)
