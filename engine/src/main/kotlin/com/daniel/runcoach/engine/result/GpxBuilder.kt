package com.daniel.runcoach.engine.result

import java.time.Instant

/** One track point for GPX export (plan P3.4). */
data class GpxPoint(val lat: Double, val lon: Double, val timeEpochMs: Long)

/**
 * Builds a GPX 1.1 document (plan P3.4). Pure, so the output can be parsed and validated in a unit test.
 */
object GpxBuilder {

    fun build(name: String, points: List<GpxPoint>, creator: String = "RunCoach"): String {
        val safeName = escape(name)
        val firstTime = points.firstOrNull()?.timeEpochMs ?: System.currentTimeMillis()
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<gpx version=\"1.1\" creator=\"$creator\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            append("  <metadata><name>$safeName</name><time>${iso(firstTime)}</time></metadata>\n")
            append("  <trk><name>$safeName</name><trkseg>\n")
            points.forEach { point ->
                append("    <trkpt lat=\"${point.lat}\" lon=\"${point.lon}\">")
                append("<time>${iso(point.timeEpochMs)}</time>")
                append("</trkpt>\n")
            }
            append("  </trkseg></trk>\n")
            append("</gpx>\n")
        }
    }

    private fun iso(epochMs: Long): String = Instant.ofEpochMilli(epochMs).toString()

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
