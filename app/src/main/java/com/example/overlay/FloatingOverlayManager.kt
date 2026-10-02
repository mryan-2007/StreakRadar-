package com.example.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import com.example.MainActivity
import com.example.data.RadarPreferences
import com.example.data.RadarStateRepository
import com.example.model.CropRegion

class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val preferences = RadarPreferences(context)

    private var floatingBadgeView: FloatingBadgeView? = null
    private var cropOverlayView: CropOverlayView? = null

    val canDrawOverlays: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    fun showFloatingBadge() {
        if (!canDrawOverlays || floatingBadgeView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        floatingBadgeView = FloatingBadgeView(
            context = context,
            windowManager = windowManager,
            layoutParams = params,
            onCropRequested = { showCropOverlay() },
            onOpenAppRequested = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        )

        try {
            windowManager.addView(floatingBadgeView, params)
        } catch (_: Exception) {}
    }

    fun hideFloatingBadge() {
        floatingBadgeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            floatingBadgeView = null
        }
    }

    fun showCropOverlay() {
        if (!canDrawOverlays || cropOverlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val currentRegion = RadarStateRepository.config.value.cropRegion

        cropOverlayView = CropOverlayView(
            context = context,
            initialRegion = currentRegion,
            onRegionSaved = { savedRegion ->
                RadarStateRepository.updateCropRegion(savedRegion)
                preferences.saveCropRegion(savedRegion)
                hideCropOverlay()
            },
            onDismiss = {
                hideCropOverlay()
            }
        ).apply {
            setInitialRegion(currentRegion)
        }

        try {
            windowManager.addView(cropOverlayView, params)
        } catch (_: Exception) {}
    }

    fun hideCropOverlay() {
        cropOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            cropOverlayView = null
        }
    }

    fun updateBadge(
        isDetecting: Boolean,
        currentStreak: Int,
        targetStreak: Int,
        lastMultiplier: Double?,
        threshold: Double
    ) {
        floatingBadgeView?.updateState(
            isDetecting = isDetecting,
            currentStreak = currentStreak,
            targetStreak = targetStreak,
            lastMultiplier = lastMultiplier,
            threshold = threshold
        )
    }

    fun destroy() {
        hideFloatingBadge()
        hideCropOverlay()
    }
}
