package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioManager
import android.media.Image
import android.media.ImageReader
import android.media.ToneGenerator
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.RadarApplication
import com.example.data.RadarStateRepository
import com.example.ocr.MultiplierDetector
import com.example.overlay.FloatingOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var detectionJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 420

    private val detector = MultiplierDetector()
    private lateinit var overlayManager: FloatingOverlayManager

    private var toneGenerator: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        overlayManager = FloatingOverlayManager(this)

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (_: Exception) {}

        observeRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_FOREGROUND -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }

                startServiceForeground()

                if (resultCode != 0 && dataIntent != null) {
                    initMediaProjection(resultCode, dataIntent)
                }

                // Show floating widget if overlay permission is granted
                if (overlayManager.canDrawOverlays) {
                    overlayManager.showFloatingBadge()
                }

                RadarStateRepository.setServiceActive(true)
                RadarStateRepository.setDetecting(true)
                startDetectionLoop()
            }

            ACTION_SHOW_CROP -> {
                overlayManager.showCropOverlay()
            }

            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startServiceForeground() {
        val notification = buildOngoingNotification(
            streak = RadarStateRepository.currentStreak.value,
            lastScore = RadarStateRepository.lastDetectedMultiplier.value,
            isDetecting = RadarStateRepository.isDetecting.value
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopSelf()
            }
        }, null)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            screenDensity = resources.configuration.densityDpi
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RadarScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun startDetectionLoop() {
        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            while (isActive) {
                val config = RadarStateRepository.config.value
                val isDetecting = RadarStateRepository.isDetecting.value

                val intervalMs = (1000L / config.checksPerSecond.coerceIn(1, 25))

                if (isDetecting && imageReader != null) {
                    processScreenFrame()
                }

                delay(intervalMs)
            }
        }
    }

    private suspend fun processScreenFrame() {
        var image: Image? = null
        try {
            image = imageReader?.acquireLatestImage() ?: return
            val planes = image.planes
            if (planes.isEmpty()) return

            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bmpWidth = screenWidth + rowPadding / pixelStride
            val fullBitmap = Bitmap.createBitmap(
                bmpWidth,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)

            val cropRegion = RadarStateRepository.config.value.cropRegion
            val cropRect = cropRegion.toPixelRect(screenWidth, screenHeight)

            // Crop the designated score strip
            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap,
                cropRect.left.coerceIn(0, fullBitmap.width - 1),
                cropRect.top.coerceIn(0, fullBitmap.height - 1),
                cropRect.width().coerceIn(10, fullBitmap.width - cropRect.left),
                cropRect.height().coerceIn(10, fullBitmap.height - cropRect.top)
            )

            fullBitmap.recycle()

            // Run OCR to detect leftmost multiplier
            val candidate = detector.detectLeftmostMultiplier(croppedBitmap)

            if (candidate != null) {
                RadarStateRepository.processDetectedScore(
                    multiplier = candidate.value,
                    rawText = candidate.rawText,
                    bitmap = croppedBitmap
                )
            } else {
                // Update preview occasionally so user sees live crop
                RadarStateRepository.updatePreviewBitmap(croppedBitmap)
            }
        } catch (_: Exception) {
            // Frame processing errors safely handled without crashing service
        } finally {
            try {
                image?.close()
            } catch (_: Exception) {}
        }
    }

    private fun observeRepository() {
        // 1. Observe alert events to trigger vibration and sound
        serviceScope.launch {
            RadarStateRepository.alertEvent.collectLatest { streak ->
                triggerAlert(streak)
            }
        }

        // 2. Observe changes to update Floating Overlay and Notification
        serviceScope.launch {
            RadarStateRepository.currentStreak.collectLatest {
                updateNotificationAndBadge()
            }
        }

        serviceScope.launch {
            RadarStateRepository.isDetecting.collectLatest {
                updateNotificationAndBadge()
            }
        }

        serviceScope.launch {
            RadarStateRepository.lastDetectedMultiplier.collectLatest {
                updateNotificationAndBadge()
            }
        }
    }

    private fun updateNotificationAndBadge() {
        val streak = RadarStateRepository.currentStreak.value
        val lastScore = RadarStateRepository.lastDetectedMultiplier.value
        val isDetecting = RadarStateRepository.isDetecting.value
        val cfg = RadarStateRepository.config.value

        overlayManager.updateBadge(
            isDetecting = isDetecting,
            currentStreak = streak,
            targetStreak = cfg.targetStreak,
            lastMultiplier = lastScore,
            threshold = cfg.threshold
        )

        val notification = buildOngoingNotification(streak, lastScore, isDetecting)
        try {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun triggerAlert(streak: Int) {
        val config = RadarStateRepository.config.value

        // Vibration alert
        if (config.vibrateOnAlert) {
            val pattern = longArrayOf(0, 350, 150, 350, 150, 600, 200, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        }

        // Sound alert
        if (config.soundOnAlert) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
            } catch (_: Exception) {}
        }

        // Show High-Priority Heads-up Notification
        showStreakTargetNotification(streak, config.threshold)
    }

    private fun showStreakTargetNotification(streak: Int, threshold: Double) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertNotif = NotificationCompat.Builder(this, RadarApplication.ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 TARGET STREAK REACHED!")
            .setContentText("$streak consecutive rounds under ${String.format("%.2fx", threshold)} detected!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .build()

        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(ALERT_NOTIFICATION_ID, alertNotif)
        } catch (_: Exception) {}
    }

    private fun buildOngoingNotification(
        streak: Int,
        lastScore: Double?,
        isDetecting: Boolean
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Start/Pause
        val toggleActionIntent = Intent(
            this,
            NotificationActionReceiver::class.java
        ).apply {
            action = if (isDetecting) NotificationActionReceiver.ACTION_PAUSE else NotificationActionReceiver.ACTION_START
        }
        val pToggle = PendingIntent.getBroadcast(
            this, 1, toggleActionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Crop
        val cropActionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CROP
        }
        val pCrop = PendingIntent.getBroadcast(
            this, 2, cropActionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Reset
        val resetActionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RESET
        }
        val pReset = PendingIntent.getBroadcast(
            this, 3, resetActionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop
        val stopActionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP
        }
        val pStop = PendingIntent.getBroadcast(
            this, 4, stopActionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scoreText = if (lastScore != null) String.format("%.2fx", lastScore) else "--"
        val statusText = if (isDetecting) "🟢 Monitoring (10 checks/s)" else "⏸ Paused"
        val target = RadarStateRepository.config.value.targetStreak
        val threshold = RadarStateRepository.config.value.threshold

        return NotificationCompat.Builder(this, RadarApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ Streak: $streak / $target | Last: $scoreText")
            .setContentText("$statusText • Target < ${String.format("%.2fx", threshold)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .addAction(
                0,
                if (isDetecting) "⏸ Pause" else "▶ Start",
                pToggle
            )
            .addAction(0, "✂ Crop Area", pCrop)
            .addAction(0, "↺ Reset", pReset)
            .addAction(0, "✕ Stop", pStop)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionJob?.cancel()
        serviceScope.cancel()

        RadarStateRepository.setServiceActive(false)
        RadarStateRepository.setDetecting(false)

        overlayManager.destroy()
        detector.close()

        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
            toneGenerator?.release()
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        const val ACTION_START_FOREGROUND = "com.example.START_CAPTURE"
        const val ACTION_SHOW_CROP = "com.example.SHOW_CROP"
        const val ACTION_STOP = "com.example.STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA_INTENT = "extra_data_intent"
    }
}
