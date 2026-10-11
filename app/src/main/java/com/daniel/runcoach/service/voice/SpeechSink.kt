package com.daniel.runcoach.service.voice

import com.daniel.runcoach.engine.voice.VoiceLanguage

/** B2: the speech surface used by [RunAnnouncer], so tests can record utterances instead of speaking. */
interface SpeechSink {
    fun init()
    fun setLanguage(language: VoiceLanguage)
    fun isLanguageAvailable(language: VoiceLanguage): Boolean
    fun speak(text: String, flush: Boolean)
    fun shutdown()
}

/** Fake sink that records every utterance (time, text, flush) for tests. */
class RecordingSpeechSink : SpeechSink {
    data class Utterance(val timeNanos: Long, val text: String, val flush: Boolean)

    val utterances = mutableListOf<Utterance>()
    var currentLanguage: VoiceLanguage = VoiceLanguage.ID

    override fun init() = Unit
    override fun setLanguage(language: VoiceLanguage) { this.currentLanguage = language }
    override fun isLanguageAvailable(language: VoiceLanguage): Boolean = true
    override fun speak(text: String, flush: Boolean) {
        utterances += Utterance(System.nanoTime(), text, flush)
    }
    override fun shutdown() = Unit
}
