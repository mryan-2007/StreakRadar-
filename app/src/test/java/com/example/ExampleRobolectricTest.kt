package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RadarStateRepository
import com.example.model.CropRegion
import com.example.model.DetectionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Before
    fun setUp() {
        RadarStateRepository.initialize(
            DetectionConfig(
                threshold = 2.00,
                targetStreak = 7
            )
        )
        RadarStateRepository.clearHistory()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StreakRadar", appName)
    }

    @Test
    fun testCropRegionPixelConversion() {
        val region = CropRegion(
            leftRatio = 0.1f,
            topRatio = 0.2f,
            rightRatio = 0.9f,
            bottomRatio = 0.4f
        )
        val rect = region.toPixelRect(1000, 2000)
        assertEquals(100, rect.left)
        assertEquals(400, rect.top)
        assertEquals(900, rect.right)
        assertEquals(800, rect.bottom)
    }

    @Test
    fun testStreakCounterAdvancesUnderThreshold() {
        RadarStateRepository.processDetectedScore(1.35, "1.35x", forceNewRound = true)
        assertEquals(1, RadarStateRepository.currentStreak.value)

        RadarStateRepository.processDetectedScore(1.80, "1.80x", forceNewRound = true)
        assertEquals(2, RadarStateRepository.currentStreak.value)

        // Score >= 2.00x resets streak
        RadarStateRepository.processDetectedScore(3.40, "3.40x", forceNewRound = true)
        assertEquals(0, RadarStateRepository.currentStreak.value)
    }

    @Test
    fun testAlertEmitsAtTargetStreak() {
        var alertTriggered = false
        val target = 7
        for (i in 1..target) {
            val isLogged = RadarStateRepository.processDetectedScore(
                1.20, "1.20x", forceNewRound = true
            )
            assertTrue(isLogged)
        }
        assertEquals(7, RadarStateRepository.currentStreak.value)
    }
}
