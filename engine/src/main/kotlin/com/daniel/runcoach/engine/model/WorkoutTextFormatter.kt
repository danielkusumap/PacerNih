package com.daniel.runcoach.engine.model

import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlin.math.floor

/**
 * F1: renders a workout's **structure only** (order, limits, targets) as plain text, e.g. for a Strava
 * description. No actual pace, status or measured distance/time. Decimal separator follows [language].
 */
object WorkoutTextFormatter {

    fun format(workout: Workout, language: VoiceLanguage): String {
        val comma = language == VoiceLanguage.ID
        val sb = StringBuilder(workout.name)
        workout.items.forEach { block ->
            when (block) {
                is Segment -> sb.append('\n').append(segmentLine(block, comma))
                is Repeat -> {
                    sb.append('\n').append(repeatHeader(block, comma))
                    block.children.forEach { child -> sb.append('\n').append("  ").append(segmentLine(child, comma)) }
                }
            }
        }
        return sb.toString()
    }

    private fun repeatHeader(repeat: Repeat, comma: Boolean): String = when {
        repeat.times != null -> "Ulangi ${repeat.times}×:"
        repeat.untilTotalDistanceM != null -> "Ulangi sampai ${distance(repeat.untilTotalDistanceM!!.toDouble(), comma)}:"
        else -> "Ulangi:"
    }

    private fun segmentLine(segment: Segment, comma: Boolean): String {
        val limit = when (val l = segment.limit) {
            is Limit.Distance -> distance(l.meters.toDouble(), comma)
            is Limit.Duration -> duration(l.seconds)
            Limit.Open -> "tanpa batas"
        }
        val target = segment.targetPaceSecPerKm?.let { " · ${target(it, segment.targetBound)}" } ?: ""
        return "${segment.label} $limit$target"
    }

    private fun distance(meters: Double, comma: Boolean): String {
        if (meters < 1000.0) return "${meters.toInt()} m"
        val km = meters / 1000.0
        if (km == floor(km)) return "${km.toInt()} km"
        val text = String.format(java.util.Locale.US, "%.1f", km)
        return (if (comma) text.replace('.', ',') else text) + " km"
    }

    private fun duration(seconds: Int): String = when {
        seconds < 60 -> "$seconds dtk"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0) "$m mnt" else "$m mnt $s dtk"
        }
        else -> "${seconds / 3600} jam ${(seconds % 3600) / 60} mnt"
    }

    private fun target(secPerKm: Int, bound: PaceTargetBound): String {
        val symbol = if (bound == PaceTargetBound.MIN) "≥" else "≤"
        return "$symbol %d:%02d /km".format(secPerKm / 60, secPerKm % 60)
    }
}
