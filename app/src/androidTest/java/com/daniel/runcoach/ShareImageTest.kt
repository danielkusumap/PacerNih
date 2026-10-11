package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.share.ShareData
import com.daniel.runcoach.share.ShareImageRenderer
import com.daniel.runcoach.share.ShareItem
import com.daniel.runcoach.share.ShareTemplate
import com.daniel.runcoach.share.ShareTextColor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** F3/F4 AC: transparent PNG with the exact template size. */
@RunWith(AndroidJUnit4::class)
class ShareImageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun sample() = ShareData(
        name = "Lari Pagi",
        dateTime = "10 Okt · 06:12",
        distanceKm = 5.03,
        movingMs = 2_052_000L,
        avgPaceSecPerKm = 408.0,
        splits = listOf("1" to 400.0, "2" to 410.0),
        passedSegments = listOf("#1" to 385.0, "#2" to 388.0),
        track = listOf(-8.58 to 116.10, -8.581 to 116.101, -8.582 to 116.103),
    )

    @Test
    fun pngIsTransparentAndCorrectlySized() {
        val selected = setOf(ShareItem.NAME, ShareItem.DISTANCE, ShareItem.TIME, ShareItem.PACE, ShareItem.SPLITS, ShareItem.SEGMENTS, ShareItem.ROUTE)
        for (template in ShareTemplate.entries) {
            val bmp = ShareImageRenderer.render(context, sample(), selected, template, ShareTextColor.WHITE)
            assertEquals(template.width, bmp.width)
            assertEquals(template.height, bmp.height)
            assertEquals("corner alpha must be 0", 0, bmp.getPixel(0, 0) ushr 24)
            assertEquals("corner alpha must be 0", 0, bmp.getPixel(template.width - 1, 0) ushr 24)
        }
    }

    @Test
    fun blackTextRendersWithoutCrash() {
        val bmp = ShareImageRenderer.render(context, sample(), setOf(ShareItem.NAME, ShareItem.DISTANCE), ShareTemplate.COMPACT, ShareTextColor.BLACK)
        assertEquals(1080, bmp.width)
    }
}
