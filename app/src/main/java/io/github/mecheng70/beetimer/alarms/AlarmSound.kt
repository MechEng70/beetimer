package io.github.mecheng70.beetimer.alarms

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import io.github.mecheng70.beetimer.R

/**
 * Plays the alarm tone (looping) and a repeating vibration, both as an alarm:
 * alarm volume, rings in silent mode and through Do Not Disturb's default alarm exception.
 * The app does this itself because some phones (OxygenOS) replace a notification's
 * sound and vibration with a single buzz.
 */
class AlarmSound(private val context: Context) {
    private var player: MediaPlayer? = null
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun start() {
        if (player != null) return
        player = startPlayer()
        val effect = VibrationEffect.createWaveform(VIBRATION, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, ALARM_ATTRS)
        }
    }

    fun stop() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        vibrator.cancel()
    }

    /**
     * The phone's alarm tone if one is set and plays, otherwise the bundled beep.
     * Some phones (OxygenOS) leave the system alarm tone unset, so the beep is the
     * fallback that always works.
     */
    private fun startPlayer(): MediaPlayer? {
        val systemTone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        if (systemTone != null) {
            runCatching { newPlayer { setDataSource(context, systemTone) } }
                .onSuccess { return it }
                .onFailure { Log.w(TAG, "System alarm tone failed, using bundled tone", it) }
        }
        return runCatching {
            newPlayer {
                context.resources.openRawResourceFd(R.raw.alarm_beep).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
        }.onFailure { Log.e(TAG, "Bundled alarm tone failed", it) }.getOrNull()
    }

    private inline fun newPlayer(source: MediaPlayer.() -> Unit): MediaPlayer {
        val p = MediaPlayer()
        try {
            p.setAudioAttributes(ALARM_ATTRS)
            p.source()
            p.isLooping = true
            p.prepare()
            p.start()
            return p
        } catch (e: Exception) {
            p.release()
            throw e
        }
    }

    private companion object {
        const val TAG = "AlarmSound"
        val VIBRATION = longArrayOf(0, 800, 400, 800, 1200)
        /** Alarm stream (Jason, 2026-11-21): the only behavior, no setting. */
        val ALARM_ATTRS: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
