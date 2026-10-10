package com.daniel.runcoach.engine.naming

import com.daniel.runcoach.engine.voice.VoiceLanguage
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSessionNamerTest {

    private val zone = "Asia/Jakarta"
    private fun at(time: String): Long =
        LocalDate.of(2026, 10, 11).atTime(LocalTime.parse(time)).atZone(ZoneId.of(zone)).toInstant().toEpochMilli()

    @Test
    fun indonesianBoundaries() {
        assertEquals("Lari Malam", DefaultSessionNamer.name(at("03:59"), zone, VoiceLanguage.ID))
        assertEquals("Lari Pagi", DefaultSessionNamer.name(at("04:00"), zone, VoiceLanguage.ID))
        assertEquals("Lari Pagi", DefaultSessionNamer.name(at("10:59"), zone, VoiceLanguage.ID))
        assertEquals("Lari Siang", DefaultSessionNamer.name(at("11:00"), zone, VoiceLanguage.ID))
        assertEquals("Lari Siang", DefaultSessionNamer.name(at("14:59"), zone, VoiceLanguage.ID))
        assertEquals("Lari Sore", DefaultSessionNamer.name(at("15:00"), zone, VoiceLanguage.ID))
        assertEquals("Lari Sore", DefaultSessionNamer.name(at("17:59"), zone, VoiceLanguage.ID))
        assertEquals("Lari Petang", DefaultSessionNamer.name(at("18:00"), zone, VoiceLanguage.ID))
        assertEquals("Lari Petang", DefaultSessionNamer.name(at("20:59"), zone, VoiceLanguage.ID))
        assertEquals("Lari Malam", DefaultSessionNamer.name(at("21:00"), zone, VoiceLanguage.ID))
    }

    @Test
    fun englishBoundaries() {
        assertEquals("Night Run", DefaultSessionNamer.name(at("03:59"), zone, VoiceLanguage.EN))
        assertEquals("Morning Run", DefaultSessionNamer.name(at("04:00"), zone, VoiceLanguage.EN))
        assertEquals("Midday Run", DefaultSessionNamer.name(at("11:00"), zone, VoiceLanguage.EN))
        assertEquals("Afternoon Run", DefaultSessionNamer.name(at("15:00"), zone, VoiceLanguage.EN))
        assertEquals("Evening Run", DefaultSessionNamer.name(at("18:00"), zone, VoiceLanguage.EN))
        assertEquals("Night Run", DefaultSessionNamer.name(at("21:00"), zone, VoiceLanguage.EN))
    }
}
