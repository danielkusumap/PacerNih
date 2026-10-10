package com.daniel.runcoach.service.voice

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Distinct vibration patterns per alert type (plan P1.4). */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun countdown() = vibrate(longArrayOf(0, 60))
    fun segmentChange() = vibrate(longArrayOf(0, 120, 80, 120))
    fun tooFast() = vibrate(longArrayOf(0, 40, 60, 40))
    fun tooSlow() = vibrate(longArrayOf(0, 180))

    private fun vibrate(pattern: LongArray) {
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
