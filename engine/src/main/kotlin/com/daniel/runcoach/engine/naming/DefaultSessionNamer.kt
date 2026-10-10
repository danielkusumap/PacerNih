package com.daniel.runcoach.engine.naming

import com.daniel.runcoach.engine.voice.VoiceLanguage
import java.time.Instant
import java.time.ZoneId

/**
 * Default session name from the start hour (plan 6.12). The stored name is final text, so changing the UI
 * language later does not rewrite history. Pure and deterministic.
 */
object DefaultSessionNamer {

    fun name(startedAtEpochMs: Long, zoneId: String, language: VoiceLanguage): String {
        val hour = Instant.ofEpochMilli(startedAtEpochMs).atZone(ZoneId.of(zoneId)).hour
        return when (hour) {
            in 4..10 -> if (language == VoiceLanguage.ID) "Lari Pagi" else "Morning Run"
            in 11..14 -> if (language == VoiceLanguage.ID) "Lari Siang" else "Midday Run"
            in 15..17 -> if (language == VoiceLanguage.ID) "Lari Sore" else "Afternoon Run"
            in 18..20 -> if (language == VoiceLanguage.ID) "Lari Petang" else "Evening Run"
            else -> if (language == VoiceLanguage.ID) "Lari Malam" else "Night Run"
        }
    }
}
