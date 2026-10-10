package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.export.GpxExporter
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** P3.4 AC: the exported file is valid GPX (parsed on device) and importable by other apps. */
@RunWith(AndroidJUnit4::class)
class GpxExportTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun exportedFileIsValidGpx() {
        val points = (0..5).map { i ->
            TrackPointEntity(
                sessionId = "gpx-session",
                idx = i,
                timeMs = i * 1000L,
                lat = -8.58 + i * 0.0001,
                lon = 116.10,
                accuracyM = 5.0,
                speedMps = 3.0,
                segmentIdx = 1,
            )
        }
        val file: File = GpxExporter.write(context, "gpx-session", "Test & Run", 1_700_000_000_000L, points)

        assertTrue("gpx file exists", file.exists())
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
        assertEquals("gpx", document.documentElement.localName)
        assertEquals(6, document.getElementsByTagName("trkpt").length)
    }
}
