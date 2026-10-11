package com.daniel.runcoach.service.voice

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import com.daniel.runcoach.engine.voice.VoiceLanguage
import java.util.Locale

/**
 * Wraps [TextToSpeech] for spoken alerts (plan P1.4). Requests transient audio focus with ducking so music
 * dips instead of stopping, and speaks through the TTS queue (single queue). [flush] preempts whatever is
 * being spoken, used for the countdown. Language falls back to English when the requested voice is missing.
 */
class TtsSpeaker(context: Context) : SpeechSink {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager? = appContext.getSystemService(AudioManager::class.java)

    private var tts: TextToSpeech? = null
    private var ready = false
    private var focusRequest: AudioFocusRequest? = null

    override fun init() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status -> ready = status == TextToSpeech.SUCCESS }
    }

    fun isReady(): Boolean = ready

    override fun setLanguage(language: VoiceLanguage) {
        val desired = if (language == VoiceLanguage.ID) Locale.forLanguageTag("id-ID") else Locale.ENGLISH
        val engine = tts ?: return
        val result = engine.setLanguage(desired)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.ENGLISH)
        }
    }

    override fun isLanguageAvailable(language: VoiceLanguage): Boolean {
        val locale = if (language == VoiceLanguage.ID) Locale.forLanguageTag("id-ID") else Locale.ENGLISH
        return tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED >= TextToSpeech.LANG_AVAILABLE
    }

    override fun speak(text: String, flush: Boolean) {
        val engine = tts ?: return
        if (!ready || text.isBlank()) return
        requestFocus()
        engine.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        abandonFocus()
    }

    private fun requestFocus() {
        val manager = audioManager ?: return
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener { }
            .build().also { focusRequest = it }
        manager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
    }

    private companion object {
        const val UTTERANCE_ID = "runcoach-alert"
    }
}
