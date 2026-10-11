package com.daniel.runcoach.service.voice

import android.content.Context
import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.runner.AlertEvent
import com.daniel.runcoach.engine.voice.PhraseBuilder
import com.daniel.runcoach.engine.voice.VoiceLanguage

/** How the countdown is announced: beeps (default) or spoken numbers (plan 6.6, S12). */
enum class CountdownStyle { BEEP, VOICE }

/**
 * Turns engine [AlertEvent]s into sound, beeps, and vibration (plan P1.4, 6.6, 6.7). Countdown uses the
 * configured [CountdownStyle], preempts ongoing speech (flush), and gets a distinct vibration. Other alerts
 * queue behind each other, so speech never overlaps.
 */
class RunAnnouncer(
    context: Context,
    private var language: VoiceLanguage = VoiceLanguage.ID,
    private var countdownStyle: CountdownStyle = CountdownStyle.BEEP,
    private var vibrateEnabled: Boolean = true,
    private val config: EngineConfig = EngineConfig(),
) {

    private val tts = TtsSpeaker(context)
    private val beep = BeepPlayer()
    private val haptics = Haptics(context)

    private fun vibrateCountdown() {
        if (vibrateEnabled) haptics.countdown()
    }

    private fun vibrateSegmentChange() {
        if (vibrateEnabled) haptics.segmentChange()
    }

    fun start() {
        tts.init()
        tts.setLanguage(language)
    }

    fun setLanguage(value: VoiceLanguage) {
        language = value
        tts.setLanguage(value)
    }

    fun setCountdownStyle(value: CountdownStyle) {
        countdownStyle = value
    }

    fun isLanguageAvailable(): Boolean = tts.isLanguageAvailable(language)

    private var pendingSegmentEnd: String? = null
    private var lastPaceSecPerKm: Double? = null
    private var lastTargetSecPerKm: Int? = null

    private fun flushPending() {
        val text = pendingSegmentEnd ?: return
        pendingSegmentEnd = null
        tts.speak(text, flush = false)
    }

    fun onEvent(event: AlertEvent) {
        // A07 (segment end) is held briefly so it can be joined with the next A03 into one utterance.
        if (event !is AlertEvent.SegmentStart && event !is AlertEvent.SegmentEnd) flushPending()
        when (event) {
            is AlertEvent.Countdown -> {
                vibrateCountdown()
                if (countdownStyle == CountdownStyle.VOICE) {
                    tts.speak(PhraseBuilder.countdownWord(event.seconds, language), flush = true)
                } else {
                    beep.tick()
                }
            }

            AlertEvent.WorkoutStarted -> {
                beep.segmentEnd()
                vibrateSegmentChange()
                tts.speak(PhraseBuilder.workoutStarted(language), flush = true)
            }

            is AlertEvent.SegmentEnd -> {
                // A07: spoken only when there is a measured pace; skipped segments are silent.
                val status = event.status
                val avg = event.avgPaceSecPerKm
                if (avg != null && status != com.daniel.runcoach.engine.result.SegmentStatus.SKIPPED) {
                    pendingSegmentEnd = PhraseBuilder.segmentEnd(avg, status ?: com.daniel.runcoach.engine.result.SegmentStatus.NO_TARGET, language)
                }
            }

            is AlertEvent.SegmentStart -> {
                vibrateSegmentChange()
                beep.tick()
                val a03 = PhraseBuilder.segmentStart(event.index, event.total ?: event.index, event.segment, language)
                val combined = pendingSegmentEnd?.let { "$it $a03" } ?: a03
                pendingSegmentEnd = null
                tts.speak(combined, flush = false)
            }

            is AlertEvent.PreSegmentWarning -> {
                tts.speak(
                    PhraseBuilder.preSegmentWarning(config.preSegmentWarningSec, event.nextKind, language),
                    flush = false,
                )
            }

            is AlertEvent.DistanceCue -> {
                vibrateSegmentChange()
                beep.tick()
                tts.speak(PhraseBuilder.distanceCue(event.nextKind, language), flush = false)
            }

            is AlertEvent.KmMarker -> {
                tts.speak(PhraseBuilder.kmMarker(event.km, event.timeSec, event.paceSecPerKm, language), flush = false)
            }

            is AlertEvent.TooSlow -> {
                lastPaceSecPerKm = event.paceLiveSecPerKm
                lastTargetSecPerKm = event.targetSecPerKm
                tts.speak(PhraseBuilder.tooSlow(event.paceLiveSecPerKm, event.targetSecPerKm, event.requiredSecPerKm, language), flush = false)
            }

            is AlertEvent.TooFast -> {
                lastPaceSecPerKm = event.paceLiveSecPerKm
                lastTargetSecPerKm = event.targetSecPerKm
                tts.speak(PhraseBuilder.tooFast(event.paceLiveSecPerKm, event.targetSecPerKm, language), flush = false)
            }

            AlertEvent.BackToStable -> {
                val pace = lastPaceSecPerKm
                val target = lastTargetSecPerKm
                if (pace != null && target != null) {
                    tts.speak(PhraseBuilder.backToStable(pace, target, language), flush = false)
                }
            }

            AlertEvent.GoalDistanceReached -> tts.speak(PhraseBuilder.goalDistanceReached(language), flush = false)
            AlertEvent.WorkoutPlanCompleted -> tts.speak(PhraseBuilder.workoutPlanCompleted(language), flush = false)
            AlertEvent.AutoPaused -> tts.speak(PhraseBuilder.autoPaused(language), flush = false)
            AlertEvent.AutoResumed -> tts.speak(PhraseBuilder.autoResumed(language), flush = false)
            AlertEvent.GpsLost -> tts.speak(PhraseBuilder.gpsLost(language), flush = false)
            AlertEvent.GpsRecovered -> tts.speak(PhraseBuilder.gpsRecovered(language), flush = false)
        }
    }

    fun shutdown() {
        tts.shutdown()
        beep.release()
    }
}
