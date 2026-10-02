package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.CropRegion

class CropOverlayView(
    context: Context,
    initialRegion: CropRegion,
    private val onRegionSaved: (CropRegion) -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val cropRectF = RectF()
    private var screenWidth = 1080
    private var screenHeight = 2400

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#B30A0F1D") // Semi-transparent dark overlay
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00E5FF") // Neon Cyan
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val guideGridPaint = Paint().apply {
        color = Color.parseColor("#6600E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#00E676") // Neon Green
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var touchMode = TOUCH_NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val cornerHandleRadius = 40f

    private lateinit var infoTextView: TextView

    companion object {
        private const val TOUCH_NONE = 0
        private const val TOUCH_DRAG_BOX = 1
        private const val TOUCH_TOP_LEFT = 2
        private const val TOUCH_TOP_RIGHT = 3
        private const val TOUCH_BOTTOM_LEFT = 4
        private const val TOUCH_BOTTOM_RIGHT = 5
        private const val TOUCH_BOTTOM_EDGE = 6
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        initControlPanel(initialRegion)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        updateRectFromRatios(CropRegion.DEFAULT_TOP_RIBBON)
    }

    fun setInitialRegion(region: CropRegion) {
        post {
            updateRectFromRatios(region)
            invalidate()
        }
    }

    private fun updateRectFromRatios(region: CropRegion) {
        if (screenWidth > 0 && screenHeight > 0) {
            cropRectF.set(
                region.leftRatio * screenWidth,
                region.topRatio * screenHeight,
                region.rightRatio * screenWidth,
                region.bottomRatio * screenHeight
            )
            updateInfoText()
        }
    }

    private fun initControlPanel(initialRegion: CropRegion) {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.parseColor("#E60F172A"))
        }

        val titleView = TextView(context).apply {
            text = "✂ DRAG & RESIZE CROP AREA OVER SCORE BAR"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        infoTextView = TextView(context).apply {
            text = "Position: Top Bar Area"
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnPreset = Button(context).apply {
            text = "Top Bar"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            textSize = 12f
            setOnClickListener {
                updateRectFromRatios(CropRegion.DEFAULT_TOP_RIBBON)
                invalidate()
            }
        }

        val btnCancel = Button(context).apply {
            text = "Cancel"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 0, 16, 0) }
            layoutParams = params
            setOnClickListener { onDismiss() }
        }

        val btnSave = Button(context).apply {
            text = "✓ Save Area"
            setBackgroundColor(Color.parseColor("#00B4D8"))
            setTextColor(Color.BLACK)
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener {
                val region = getCurrentCropRegion()
                onRegionSaved(region)
            }
        }

        buttonRow.addView(btnPreset)
        buttonRow.addView(btnCancel)
        buttonRow.addView(btnSave)

        rootLayout.addView(titleView)
        rootLayout.addView(infoTextView)
        rootLayout.addView(buttonRow)

        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        addView(rootLayout, layoutParams)
    }

    private fun getCurrentCropRegion(): CropRegion {
        val w = screenWidth.toFloat().coerceAtLeast(1f)
        val h = screenHeight.toFloat().coerceAtLeast(1f)

        return CropRegion(
            leftRatio = (cropRectF.left / w).coerceIn(0f, 0.95f),
            topRatio = (cropRectF.top / h).coerceIn(0f, 0.95f),
            rightRatio = (cropRectF.right / w).coerceIn(0.05f, 1f),
            bottomRatio = (cropRectF.bottom / h).coerceIn(0.05f, 1f)
        )
    }

    private fun updateInfoText() {
        if (::infoTextView.isInitialized) {
            val reg = getCurrentCropRegion()
            val wPercent = ((reg.rightRatio - reg.leftRatio) * 100).toInt()
            val hPercent = ((reg.bottomRatio - reg.topRatio) * 100).toInt()
            val topPercent = (reg.topRatio * 100).toInt()
            infoTextView.text = "Top: ${topPercent}% | Size: ${wPercent}% × ${hPercent}%"
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw darkened overlay
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), maskPaint)

        // 2. Clear crop rectangle area
        canvas.drawRect(cropRectF, clearPaint)

        // 3. Draw dashed guide grid inside crop area
        val midY = cropRectF.centerY()
        canvas.drawLine(cropRectF.left, midY, cropRectF.right, midY, guideGridPaint)

        // 4. Draw vibrant border
        canvas.drawRect(cropRectF, borderPaint)

        // 5. Draw 4 green corner handles
        canvas.drawCircle(cropRectF.left, cropRectF.top, 24f, cornerPaint)
        canvas.drawCircle(cropRectF.right, cropRectF.top, 24f, cornerPaint)
        canvas.drawCircle(cropRectF.left, cropRectF.bottom, 24f, cornerPaint)
        canvas.drawCircle(cropRectF.right, cropRectF.bottom, 24f, cornerPaint)
        // Center bottom handle for height dragging
        canvas.drawCircle(cropRectF.centerX(), cropRectF.bottom, 20f, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y

                touchMode = when {
                    isNear(x, y, cropRectF.left, cropRectF.top) -> TOUCH_TOP_LEFT
                    isNear(x, y, cropRectF.right, cropRectF.top) -> TOUCH_TOP_RIGHT
                    isNear(x, y, cropRectF.left, cropRectF.bottom) -> TOUCH_BOTTOM_LEFT
                    isNear(x, y, cropRectF.right, cropRectF.bottom) -> TOUCH_BOTTOM_RIGHT
                    isNear(x, y, cropRectF.centerX(), cropRectF.bottom) -> TOUCH_BOTTOM_EDGE
                    cropRectF.contains(x, y) -> TOUCH_DRAG_BOX
                    else -> TOUCH_NONE
                }
                return touchMode != TOUCH_NONE || super.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                val minSize = 60f

                when (touchMode) {
                    TOUCH_DRAG_BOX -> {
                        val newLeft = (cropRectF.left + dx).coerceIn(0f, screenWidth - cropRectF.width())
                        val newTop = (cropRectF.top + dy).coerceIn(0f, screenHeight - cropRectF.height())
                        cropRectF.offsetTo(newLeft, newTop)
                    }
                    TOUCH_TOP_LEFT -> {
                        cropRectF.left = (cropRectF.left + dx).coerceIn(0f, cropRectF.right - minSize)
                        cropRectF.top = (cropRectF.top + dy).coerceIn(0f, cropRectF.bottom - minSize)
                    }
                    TOUCH_TOP_RIGHT -> {
                        cropRectF.right = (cropRectF.right + dx).coerceIn(cropRectF.left + minSize, screenWidth.toFloat())
                        cropRectF.top = (cropRectF.top + dy).coerceIn(0f, cropRectF.bottom - minSize)
                    }
                    TOUCH_BOTTOM_LEFT -> {
                        cropRectF.left = (cropRectF.left + dx).coerceIn(0f, cropRectF.right - minSize)
                        cropRectF.bottom = (cropRectF.bottom + dy).coerceIn(cropRectF.top + minSize, screenHeight.toFloat())
                    }
                    TOUCH_BOTTOM_RIGHT -> {
                        cropRectF.right = (cropRectF.right + dx).coerceIn(cropRectF.left + minSize, screenWidth.toFloat())
                        cropRectF.bottom = (cropRectF.bottom + dy).coerceIn(cropRectF.top + minSize, screenHeight.toFloat())
                    }
                    TOUCH_BOTTOM_EDGE -> {
                        cropRectF.bottom = (cropRectF.bottom + dy).coerceIn(cropRectF.top + minSize, screenHeight.toFloat())
                    }
                }

                lastTouchX = x
                lastTouchY = y
                updateInfoText()
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TOUCH_NONE
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun isNear(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x1 - x2
        val dy = y1 - y2
        return (dx * dx + dy * dy) <= (cornerHandleRadius * cornerHandleRadius * 4)
    }
}
