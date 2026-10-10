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

    fun onEvent(event: AlertEvent) {
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

            is AlertEvent.SegmentStart -> {
                vibrateSegmentChange()
                beep.tick()
                tts.speak(
                    PhraseBuilder.segmentStart(event.index, event.total ?: event.index, event.segment, language),
                    flush = false,
                )
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

            AlertEvent.GoalDistanceReached -> tts.speak(PhraseBuilder.goalDistanceReached(language), flush = false)
            AlertEvent.WorkoutPlanCompleted -> tts.speak(PhraseBuilder.workoutPlanCompleted(language), flush = false)
            AlertEvent.AutoPaused -> tts.speak(PhraseBuilder.autoPaused(language), flush = false)
            AlertEvent.AutoResumed -> tts.speak(PhraseBuilder.autoResumed(language), flush = false)
            AlertEvent.GpsLost -> tts.speak(PhraseBuilder.gpsLost(language), flush = false)
            AlertEvent.GpsRecovered -> tts.speak(PhraseBuilder.gpsRecovered(language), flush = false)
            else -> Unit
        }
    }

    fun shutdown() {
        tts.shutdown()
        beep.release()
    }
}
