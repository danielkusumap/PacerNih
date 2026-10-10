package com.daniel.runcoach.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.engine.result.GpxBuilder
import com.daniel.runcoach.engine.result.GpxPoint
import java.io.File

/** Writes a session's track to a GPX file and builds a share intent (plan P3.4). */
object GpxExporter {

    fun write(
        context: Context,
        sessionId: String,
        sessionName: String,
        startedAtEpochMs: Long,
        points: List<TrackPointEntity>,
    ): File {
        val directory = File(context.cacheDir, "gpx").apply { mkdirs() }
        val file = File(directory, "$sessionId.gpx")
        val firstTimeMs = points.firstOrNull()?.timeMs ?: 0L
        val gpxPoints = points.map {
            GpxPoint(it.lat, it.lon, startedAtEpochMs + (it.timeMs - firstTimeMs))
        }
        file.writeText(GpxBuilder.build(sessionName, gpxPoints))
        return file
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
