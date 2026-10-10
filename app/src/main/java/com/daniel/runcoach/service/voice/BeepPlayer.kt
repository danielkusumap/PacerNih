package com.daniel.runcoach.service.voice

import android.media.AudioManager
import android.media.ToneGenerator

/** Low-latency tones for countdown ticks and segment cues (plan P1.4), using the built-in [ToneGenerator]. */
class BeepPlayer {

    private var tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME) }.getOrNull()

    /** Short tick for a countdown step or a distance cue. */
    fun tick() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, TICK_MS)
    }

    /** Longer, higher-pitched tone marking the end of a countdown or the start of the run. */
    fun segmentEnd() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, END_MS)
    }

    fun release() {
        tone?.release()
        tone = null
    }

    private companion object {
        const val VOLUME = 80
        const val TICK_MS = 150
        const val END_MS = 400
    }
}
