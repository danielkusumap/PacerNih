package com.daniel.runcoach.engine.result

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpxBuilderTest {

    @Test
    fun producesValidGpxParsedByAnXmlParser() {
        val points = listOf(
            GpxPoint(-8.58, 116.10, 1_700_000_000_000L),
            GpxPoint(-8.581, 116.101, 1_700_000_001_000L),
        )
        val gpx = GpxBuilder.build("Lari Pagi & sore", points)

        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(gpx.toByteArray()))

        val root = document.documentElement
        assertEquals("gpx", root.localName)
        assertEquals("1.1", root.getAttribute("version"))
        val trackPoints = document.getElementsByTagName("trkpt")
        assertEquals(2, trackPoints.length)
        val first = trackPoints.item(0)
        assertEquals("-8.58", first.attributes.getNamedItem("lat").nodeValue)
        assertEquals("-8.581", trackPoints.item(1).attributes.getNamedItem("lat").nodeValue)
    }

    @Test
    fun escapesSpecialCharactersInNames() {
        val gpx = GpxBuilder.build("A < B & C", listOf(GpxPoint(0.0, 0.0, 0L)))
        assertTrue(gpx.contains("A &lt; B &amp; C"))
    }
}
