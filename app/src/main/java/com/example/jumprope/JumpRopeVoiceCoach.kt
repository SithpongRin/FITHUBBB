package com.example.jumprope

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class JumpRopeVoiceCoach(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isKhmerTtsSupported = false
    private var toneGenerator: ToneGenerator? = null

    // Khmer digits map for localized numbers
    private val khmerDigits = listOf("០", "១", "២", "៣", "៤", "៥", "៦", "៧", "៨", "៩")

    // Spoken numbers in Khmer
    private val khmerNumbersUnderTen = mapOf(
        1 to "មួយ", 2 to "ពីរ", 3 to "បី", 4 to "បួន", 5 to "ប្រាំ",
        6 to "ប្រាំមួយ", 7 to "ប្រាំពីរ", 8 to "ប្រាំបី", 9 to "ប្រាំបួន", 10 to "ដប់"
    )

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (_: Throwable) {}

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val kmLocale = Locale("km", "KH")
                val avail = tts?.isLanguageAvailable(kmLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                isKhmerTtsSupported = avail >= TextToSpeech.LANG_AVAILABLE
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Throwable) {}
    }

    /**
     * Announces a jump count (for Every Jump mode).
     * Uses QUEUE_FLUSH with audio queue protection so TTS does not accumulate a delayed backlog at high cadence.
     */
    fun speakCount(count: Int, language: VoiceLanguage) {
        if (!isTtsReady || count <= 0) return

        val text = if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
            khmerNumbersUnderTen[count] ?: count.toString()
        } else {
            count.toString()
        }

        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jump_count_$count")
        } catch (_: Throwable) {}
    }

    /**
     * Announces a milestone achievement.
     */
    fun speakMilestone(count: Int, language: VoiceLanguage) {
        if (!isTtsReady) return

        val text = if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
            "លោតបាន $count ដងហើយ"
        } else {
            "$count jumps completed!"
        }

        playMilestoneAlert()
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "milestone_$count")
        } catch (_: Throwable) {}
    }

    /**
     * Target reached voice:
     * English: "Target reached. Time to rest." / "Round complete. Rest for X seconds."
     * Khmer: "ដល់គោលដៅហើយ។ ដល់ពេលសម្រាក។" / "ជុំទីនេះបានបញ្ចប់។ សូមសម្រាក X វិនាទី។"
     */
    fun speakTargetReached(restSeconds: Int, isFinalRound: Boolean, language: VoiceLanguage) {
        if (!isTtsReady) return

        val text = if (isFinalRound) {
            if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
                "ការហាត់ប្រាណបានបញ្ចប់។"
            } else {
                "Workout complete."
            }
        } else {
            if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
                if (restSeconds > 0) "ជុំទីនេះបានបញ្ចប់។ សូមសម្រាក $restSeconds វិនាទី។"
                else "ដល់គោលដៅហើយ។ ដល់ពេលសម្រាក។"
            } else {
                if (restSeconds > 0) "Round complete. Rest for $restSeconds seconds."
                else "Target reached. Time to rest."
            }
        }

        playMilestoneAlert()
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "target_reached")
        } catch (_: Throwable) {}
    }

    /**
     * Work duration reached in Time mode.
     */
    fun speakWorkTimeComplete(restSeconds: Int, isFinalRound: Boolean, language: VoiceLanguage) {
        speakTargetReached(restSeconds, isFinalRound, language)
    }

    /**
     * After rest finishes:
     * English: "Rest complete. Get ready for the next round."
     * Khmer: "ការសម្រាកបានបញ្ចប់។ ត្រៀមសម្រាប់ជុំបន្ទាប់។"
     */
    fun speakRestComplete(nextRound: Int = 1, language: VoiceLanguage) {
        if (!isTtsReady) return

        val text = if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
            "ការសម្រាកបានបញ្ចប់។ ត្រៀមសម្រាប់ជុំបន្ទាប់។"
        } else {
            "Rest complete. Get ready for the next round."
        }

        playChime()
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rest_complete")
        } catch (_: Throwable) {}
    }

    /**
     * Workout fully complete:
     * English: "Workout complete."
     * Khmer: "ការហាត់ប្រាណបានបញ្ចប់។"
     */
    fun speakWorkoutComplete(language: VoiceLanguage) {
        if (!isTtsReady) return

        val text = if (language == VoiceLanguage.KHMER && isKhmerTtsSupported) {
            "ការហាត់ប្រាណបានបញ្ចប់។"
        } else {
            "Workout complete."
        }

        playMilestoneAlert()
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "workout_complete")
        } catch (_: Throwable) {}
    }

    fun playChime() {
        try {
            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notifUri)
            ringtone?.play()
        } catch (_: Throwable) {}
    }

    fun playMilestoneAlert() {
        playChime()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        } catch (_: Throwable) {}
        triggerHaptic()
    }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(longArrayOf(0, 150, 100, 250), -1)
                }
            }
        } catch (_: Throwable) {}
    }
}
