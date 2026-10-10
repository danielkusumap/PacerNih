package com.daniel.runcoach.engine.voice

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.roundHalfUp

enum class VoiceLanguage { ID, EN }

/**
 * Builds the spoken phrases for every alert (plan 6.7, F20). Pure text; no audio. All numbers follow the
 * F20 rounding and wording rules, so a snapshot test can pin them down.
 */
object PhraseBuilder {

    fun workoutStarted(language: VoiceLanguage): String =
        if (language == VoiceLanguage.ID) "Mulai." else "Go."

    fun segmentStart(index: Int, total: Int, segment: Segment, language: VoiceLanguage): String {
        val head = if (language == VoiceLanguage.ID) "Segmen $index dari $total." else "Segment $index of $total."
        val type = capitalize(segmentKind(segment.kind, language))
        val limit = segmentLimit(segment.limit, language)
        val target = segment.targetPaceSecPerKm
            ?.let { "Target pace ${pace(it.toDouble(), language)}." }
            ?: (if (language == VoiceLanguage.ID) "Tanpa target." else "No target.")
        return "$head $type $limit. $target"
    }

    fun tooSlow(
        paceLiveSecPerKm: Double,
        targetSecPerKm: Int,
        requiredSecPerKm: Double?,
        language: VoiceLanguage,
    ): String {
        val live = pace(paceLiveSecPerKm, language)
        val target = pace(targetSecPerKm.toDouble(), language)
        return if (language == VoiceLanguage.ID) {
            val need = requiredSecPerKm?.let { " Butuh ${pace(it, language)} untuk sisa jarak ini." } ?: ""
            "Terlalu lambat. Pace sekarang $live, target $target.$need"
        } else {
            val need = requiredSecPerKm?.let { " You need ${pace(it, language)} for the rest of this stretch." } ?: ""
            "Too slow. Current pace $live, target $target.$need"
        }
    }

    fun tooFast(paceLiveSecPerKm: Double, targetSecPerKm: Int, language: VoiceLanguage): String {
        val live = pace(paceLiveSecPerKm, language)
        val target = pace(targetSecPerKm.toDouble(), language)
        return if (language == VoiceLanguage.ID) {
            "Terlalu cepat. Pace sekarang $live, target $target."
        } else {
            "Too fast. Current pace $live, target $target."
        }
    }

    fun backToStable(paceLiveSecPerKm: Double, targetSecPerKm: Int, language: VoiceLanguage): String {
        val live = pace(paceLiveSecPerKm, language)
        val target = pace(targetSecPerKm.toDouble(), language)
        return if (language == VoiceLanguage.ID) {
            "Pace stabil. Sekarang $live, target $target."
        } else {
            "Pace is steady. Now $live, target $target."
        }
    }

    fun segmentEnd(avgPaceSecPerKm: Double, status: SegmentStatus, language: VoiceLanguage): String {
        val avg = pace(avgPaceSecPerKm, language)
        val word = statusWord(status, language)
        return if (language == VoiceLanguage.ID) {
            "Segmen selesai. Rata-rata $avg. $word."
        } else {
            "Segment done. Average $avg. $word."
        }
    }

    fun kmMarker(km: Int, timeSec: Double, paceSecPerKm: Double, language: VoiceLanguage): String {
        val time = spokenTime(timeSec, language)
        val paceText = pace(paceSecPerKm, language)
        return if (language == VoiceLanguage.ID) {
            "$km kilometer. Waktu $time. Pace $paceText."
        } else {
            "$km ${if (km == 1) "kilometer" else "kilometers"}. Time $time. Pace $paceText."
        }
    }

    fun goalDistanceReached(language: VoiceLanguage): String =
        if (language == VoiceLanguage.ID) "Target jarak tercapai. Lanjut atau berhenti kapan saja."
        else "Distance goal reached. Keep going or stop anytime."

    fun workoutPlanCompleted(language: VoiceLanguage): String =
        if (language == VoiceLanguage.ID) "Rencana workout selesai." else "Workout plan complete."

    fun preSegmentWarning(seconds: Int, nextKind: SegmentKind, language: VoiceLanguage): String =
        if (language == VoiceLanguage.ID) "$seconds detik lagi ganti ke ${segmentKind(nextKind, language)}."
        else "$seconds seconds until ${segmentKind(nextKind, language)}."

    fun distanceCue(nextKind: SegmentKind, language: VoiceLanguage): String =
        if (language == VoiceLanguage.ID) "Siap-siap. Sebentar lagi ganti ke ${segmentKind(nextKind, language)}."
        else "Get ready. Switching to ${segmentKind(nextKind, language)} soon."

    fun countdownWord(n: Int, language: VoiceLanguage): String {
        val id = listOf("nol", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan", "sembilan", "sepuluh")
        val en = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        val table = if (language == VoiceLanguage.ID) id else en
        return table.getOrElse(n) { n.toString() }
    }

    /** A pace or duration in seconds: "{m} menit pas", "{m} menit {y} detik", "{m} menit {y}" (F20). */
    fun pace(paceSecPerKm: Double, language: VoiceLanguage): String =
        spokenTime(roundHalfUp(paceSecPerKm).toDouble(), language)

    /** Spoken time. Durations of an hour or more switch to hours/minutes (F20). */
    fun spokenTime(totalSec: Double, language: VoiceLanguage): String {
        val seconds = roundHalfUp(totalSec).toInt()
        if (seconds >= 3600) {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            return if (language == VoiceLanguage.ID) "$hours jam $minutes menit"
            else "$hours hours $minutes minutes"
        }
        val minutes = seconds / 60
        val remainder = seconds % 60
        return if (language == VoiceLanguage.ID) {
            when {
                remainder == 0 -> "$minutes menit pas"
                remainder in 1..9 -> "$minutes menit $remainder detik"
                else -> "$minutes menit $remainder"
            }
        } else {
            if (remainder == 0) "$minutes minutes flat" else "$minutes minutes $remainder seconds"
        }
    }

    /** Distance wording for a segment limit (F20): meters under 1 km, else kilometres with one decimal. */
    fun distance(meters: Double, language: VoiceLanguage): String {
        val whole = roundHalfUp(meters).toInt()
        if (whole < 1000) return if (language == VoiceLanguage.ID) "$whole meter" else "$whole meters"
        if (whole % 1000 == 0) {
            val km = whole / 1000
            return if (language == VoiceLanguage.ID) "$km kilometer" else "$km kilometers"
        }
        val tenths = roundHalfUp(meters / 100.0).toInt()
        val kmWhole = tenths / 10
        val fraction = tenths % 10
        return if (language == VoiceLanguage.ID) {
            "$kmWhole koma $fraction kilometer"
        } else {
            "$kmWhole point $fraction kilometers"
        }
    }

    fun segmentKind(kind: SegmentKind, language: VoiceLanguage): String = if (language == VoiceLanguage.ID) {
        when (kind) {
            SegmentKind.WARMUP -> "pemanasan"
            SegmentKind.RUN -> "lari"
            SegmentKind.WALK -> "jalan"
            SegmentKind.RECOVERY -> "pemulihan"
            SegmentKind.COOLDOWN -> "pendinginan"
        }
    } else {
        when (kind) {
            SegmentKind.WARMUP -> "warm up"
            SegmentKind.RUN -> "run"
            SegmentKind.WALK -> "walk"
            SegmentKind.RECOVERY -> "recovery"
            SegmentKind.COOLDOWN -> "cool down"
        }
    }

    private fun statusWord(status: SegmentStatus, language: VoiceLanguage): String = if (language == VoiceLanguage.ID) {
        when (status) {
            SegmentStatus.PASS -> "Lulus"
            SegmentStatus.FAIL -> "Gagal"
            SegmentStatus.BORDERLINE -> "Borderline"
            SegmentStatus.NO_TARGET -> "Tanpa target"
            SegmentStatus.NO_DATA -> "Data kurang"
            SegmentStatus.SKIPPED -> "Dilewati"
        }
    } else {
        when (status) {
            SegmentStatus.PASS -> "Passed"
            SegmentStatus.FAIL -> "Failed"
            SegmentStatus.BORDERLINE -> "Borderline"
            SegmentStatus.NO_TARGET -> "No target"
            SegmentStatus.NO_DATA -> "Not enough data"
            SegmentStatus.SKIPPED -> "Skipped"
        }
    }

    private fun segmentLimit(limit: Limit, language: VoiceLanguage): String = when (limit) {
        is Limit.Distance -> distance(limit.meters.toDouble(), language)
        is Limit.Duration -> spokenSegmentDuration(limit.seconds, language)
        Limit.Open -> if (language == VoiceLanguage.ID) "tanpa batas" else "open ended"
    }

    private fun spokenSegmentDuration(seconds: Int, language: VoiceLanguage): String {
        val minutes = seconds / 60
        val remainder = seconds % 60
        return if (language == VoiceLanguage.ID) {
            when {
                minutes == 0 -> "$remainder detik"
                remainder == 0 -> "$minutes menit"
                else -> "$minutes menit $remainder detik"
            }
        } else {
            when {
                minutes == 0 -> "$remainder seconds"
                remainder == 0 -> "$minutes minutes"
                else -> "$minutes minutes $remainder seconds"
            }
        }
    }

    private fun capitalize(text: String): String =
        if (text.isEmpty()) text else text[0].uppercaseChar() + text.substring(1)
}
