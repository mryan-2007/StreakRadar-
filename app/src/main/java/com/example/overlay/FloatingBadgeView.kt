package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.RadarStateRepository

@SuppressLint("ViewConstructor")
class FloatingBadgeView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onCropRequested: () -> Unit,
    private val onOpenAppRequested: () -> Unit
) : FrameLayout(context) {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var isExpanded = false

    private lateinit var minimizedContainer: LinearLayout
    private lateinit var expandedContainer: LinearLayout

    private lateinit var tvMiniStatus: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvLastScore: TextView
    private lateinit var btnToggleDetect: Button

    init {
        setupViews()
    }

    private fun setupViews() {
        // --- 1. Minimized View (Compact Pill) ---
        minimizedContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 16, 28, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E60F172A"))
                cornerRadius = 60f
                setStroke(3, Color.parseColor("#00E5FF"))
            }
        }

        tvMiniStatus = TextView(context).apply {
            text = "⚡ 0/7 | --"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        minimizedContainer.addView(tvMiniStatus)

        // --- 2. Expanded View (Full Control Card) ---
        expandedContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#00E5FF"))
            }
        }

        // Header Row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvTitle = TextView(context).apply {
            text = "⚡ StreakRadar HUD"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = p
        }
        val btnClose = TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(16, 8, 16, 8)
            setOnClickListener { toggleExpanded(false) }
        }
        headerRow.addView(tvTitle)
        headerRow.addView(btnClose)

        // Status Row
        tvStreakCount = TextView(context).apply {
            text = "Streak: 0 / 7 (< 2.00x)"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 16, 0, 4)
        }

        tvLastScore = TextView(context).apply {
            text = "Last: None"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }

        // Action Buttons Row
        val btnRow1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        btnToggleDetect = Button(context).apply {
            text = "⏸ Pause"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#00B4D8"))
            setTextColor(Color.BLACK)
            setOnClickListener {
                val current = RadarStateRepository.isDetecting.value
                RadarStateRepository.setDetecting(!current)
            }
        }

        val btnCrop = Button(context).apply {
            text = "✂ Crop"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(12, 0, 12, 0) }
            layoutParams = p
            setOnClickListener {
                toggleExpanded(false)
                onCropRequested()
            }
        }

        val btnReset = Button(context).apply {
            text = "↺ Reset"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                RadarStateRepository.resetStreak()
            }
        }

        btnRow1.addView(btnToggleDetect)
        btnRow1.addView(btnCrop)
        btnRow1.addView(btnReset)

        expandedContainer.addView(headerRow)
        expandedContainer.addView(tvStreakCount)
        expandedContainer.addView(tvLastScore)
        expandedContainer.addView(btnRow1)

        addView(minimizedContainer)
        addView(expandedContainer)

        setupTouchListener()
    }

    private fun setupTouchListener() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(this, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Click event: toggle expand
                        toggleExpanded(!isExpanded)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        if (expand) {
            minimizedContainer.visibility = View.GONE
            expandedContainer.visibility = View.VISIBLE
        } else {
            expandedContainer.visibility = View.GONE
            minimizedContainer.visibility = View.VISIBLE
        }
        windowManager.updateViewLayout(this, layoutParams)
    }

    fun updateState(
        isDetecting: Boolean,
        currentStreak: Int,
        targetStreak: Int,
        lastMultiplier: Double?,
        threshold: Double
    ) {
        post {
            val scoreStr = if (lastMultiplier != null) String.format("%.2fx", lastMultiplier) else "--"
            val statusIcon = if (isDetecting) "⚡" else "⏸"

            tvMiniStatus.text = "$statusIcon $currentStreak/$targetStreak | $scoreStr"

            val isGoal = currentStreak >= targetStreak
            val streakColor = if (isGoal) Color.parseColor("#00E676") else Color.parseColor("#00E5FF")
            tvMiniStatus.setTextColor(streakColor)

            tvStreakCount.text = "Streak: $currentStreak / $targetStreak (Target: < ${String.format("%.2fx", threshold)})"
            tvLastScore.text = "Last: $scoreStr"

            btnToggleDetect.text = if (isDetecting) "⏸ Pause" else "▶ Start"
            btnToggleDetect.setBackgroundColor(
                if (isDetecting) Color.parseColor("#EF4444") else Color.parseColor("#00E676")
            )
        }
    }
}
