package com.example.model

/**
 * Region of screen to crop, stored as normalized relative coordinates (0.0 to 1.0)
 * to work consistently across screen resolutions and orientations.
 */
data class CropRegion(
    val leftRatio: Float = 0.02f,    // 2% from left
    val topRatio: Float = 0.08f,     // 8% from top (typical browser score ribbon)
    val rightRatio: Float = 0.98f,   // 98% from left
    val bottomRatio: Float = 0.16f   // 16% from top (~8% height strip)
) {
    val widthRatio: Float
        get() = (rightRatio - leftRatio).coerceAtLeast(0.05f)

    val heightRatio: Float
        get() = (bottomRatio - topRatio).coerceAtLeast(0.02f)

    fun toPixelRect(screenWidth: Int, screenHeight: Int): android.graphics.Rect {
        val left = (leftRatio * screenWidth).toInt().coerceIn(0, screenWidth - 10)
        val top = (topRatio * screenHeight).toInt().coerceIn(0, screenHeight - 10)
        val right = (rightRatio * screenWidth).toInt().coerceIn(left + 10, screenWidth)
        val bottom = (bottomRatio * screenHeight).toInt().coerceIn(top + 10, screenHeight)
        return android.graphics.Rect(left, top, right, bottom)
    }

    companion object {
        val DEFAULT_TOP_RIBBON = CropRegion(
            leftRatio = 0.02f,
            topRatio = 0.08f,
            rightRatio = 0.98f,
            bottomRatio = 0.16f
        )
    }
}
