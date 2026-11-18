package com.example.tools

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Lightweight, fault-tolerant audio feedback engine for GateX Labs:
 * - Piezo buzzer beep when a BUZZER component is energized during simulation
 * - Subtle tactile click when flipping switches or snapping wires
 * - Short warning tone when a fuse blows or short-circuit occurs
 */
object LabAudioFeedback {
    @Volatile
    private var toneGen: ToneGenerator? = null

    @Volatile
    private var lastBuzzerMs: Long = 0L

    private fun getOrCreate(): ToneGenerator? {
        toneGen?.let { return it }
        return runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, 65).also { toneGen = it }
        }.getOrNull()
    }

    fun playBuzzerBeep(enabled: Boolean) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - lastBuzzerMs < 380L) return
        lastBuzzerMs = now
        runCatching {
            getOrCreate()?.startTone(ToneGenerator.TONE_DTMF_D, 160)
        }
    }

    fun playSwitchClick(enabled: Boolean) {
        if (!enabled) return
        runCatching {
            getOrCreate()?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        }
    }

    fun playWarningTone(enabled: Boolean) {
        if (!enabled) return
        runCatching {
            getOrCreate()?.startTone(ToneGenerator.TONE_SUP_ERROR, 140)
        }
    }
}
