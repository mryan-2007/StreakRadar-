package com.example.data

import android.graphics.Bitmap
import com.example.model.CropRegion
import com.example.model.DetectionConfig
import com.example.model.StreakRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object RadarStateRepository {

    private val _config = MutableStateFlow(DetectionConfig())
    val config: StateFlow<DetectionConfig> = _config.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _peakStreak = MutableStateFlow(0)
    val peakStreak: StateFlow<Int> = _peakStreak.asStateFlow()

    private val _lastDetectedMultiplier = MutableStateFlow<Double?>(null)
    val lastDetectedMultiplier: StateFlow<Double?> = _lastDetectedMultiplier.asStateFlow()

    private val _lastDetectedText = MutableStateFlow("")
    val lastDetectedText: StateFlow<String> = _lastDetectedText.asStateFlow()

    private val _lastCroppedBitmapPreview = MutableStateFlow<Bitmap?>(null)
    val lastCroppedBitmapPreview: StateFlow<Bitmap?> = _lastCroppedBitmapPreview.asStateFlow()

    private val _recentRecords = MutableStateFlow<List<StreakRecord>>(emptyList())
    val recentRecords: StateFlow<List<StreakRecord>> = _recentRecords.asStateFlow()

    private val _alertEvent = MutableSharedFlow<Int>(extraBufferCapacity = 5)
    val alertEvent: SharedFlow<Int> = _alertEvent.asSharedFlow()

    private var lastRecordedTimestamp = 0L
    private var lastRecordedMultiplier: Double? = null

    fun initialize(savedConfig: DetectionConfig) {
        _config.value = savedConfig
    }

    fun updateConfig(newConfig: DetectionConfig) {
        _config.value = newConfig
    }

    fun updateCropRegion(region: CropRegion) {
        _config.value = _config.value.copy(cropRegion = region)
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
        if (!active) {
            _isDetecting.value = false
        }
    }

    fun setDetecting(detecting: Boolean) {
        _isDetecting.value = detecting
    }

    fun resetStreak() {
        _currentStreak.value = 0
    }

    fun clearHistory() {
        _recentRecords.value = emptyList()
        _currentStreak.value = 0
        _peakStreak.value = 0
        _lastDetectedMultiplier.value = null
        _lastDetectedText.value = ""
        lastRecordedMultiplier = null
    }

    fun updatePreviewBitmap(bitmap: Bitmap?) {
        _lastCroppedBitmapPreview.value = bitmap
    }

    /**
     * Process a detected leftmost score.
     * Evaluates whether it's a genuine new round by checking difference and debounce time.
     */
    fun processDetectedScore(
        multiplier: Double,
        rawText: String,
        bitmap: Bitmap? = null,
        forceNewRound: Boolean = false
    ): Boolean {
        val now = System.currentTimeMillis()
        val cfg = _config.value

        // Prevent duplicate detections of the exact same number within debounce window
        val isDuplicate = !forceNewRound &&
                lastRecordedMultiplier != null &&
                kotlin.math.abs(lastRecordedMultiplier!! - multiplier) < 0.009 &&
                (now - lastRecordedTimestamp) < cfg.minRoundDebounceMs

        if (isDuplicate) {
            return false
        }

        lastRecordedTimestamp = now
        lastRecordedMultiplier = multiplier
        _lastDetectedMultiplier.value = multiplier
        _lastDetectedText.value = rawText
        if (bitmap != null) {
            _lastCroppedBitmapPreview.value = bitmap
        }

        val isUnderThreshold = multiplier < cfg.threshold
        val newStreak = if (isUnderThreshold) {
            _currentStreak.value + 1
        } else {
            0
        }

        _currentStreak.value = newStreak
        if (newStreak > _peakStreak.value) {
            _peakStreak.value = newStreak
        }

        val record = StreakRecord(
            multiplier = multiplier,
            isUnderThreshold = isUnderThreshold,
            streakAfterRound = newStreak,
            rawText = rawText
        )

        // Keep last 40 rounds
        val currentList = _recentRecords.value.toMutableList()
        currentList.add(0, record)
        if (currentList.size > 40) {
            currentList.removeAt(currentList.lastIndex)
        }
        _recentRecords.value = currentList

        // Check streak threshold
        if (isUnderThreshold && newStreak >= cfg.targetStreak) {
            _alertEvent.tryEmit(newStreak)
        }

        return true
    }
}
