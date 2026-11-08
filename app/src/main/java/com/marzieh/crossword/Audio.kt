package com.marzieh.crossword

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class Sfx { Click, Correct, Wrong, Coin, Win }

val TrackNames = listOf("شاد و کودکانه", "آرام و تمرکزی", "فانتزی", "ماجراجویانه", "مراحل سخت", "شب آرام")
private val TrackRes = listOf(R.raw.music_1, R.raw.music_2, R.raw.music_3, R.raw.music_4, R.raw.music_5, R.raw.music_6)

/** موسیقی، افکت‌ها و گفتار فارسی شخصیت (کاملاً مستقل از هم: قطع موسیقی صدای شخصیت را قطع نمی‌کند). */
class Audio(private val ctx: Context, private val store: Store) {
    private val main = Handler(Looper.getMainLooper())
    private var mp: MediaPlayer? = null
    private var curTrack = -1
    private var pausedByApp = false

    private val pool: SoundPool = SoundPool.Builder().setMaxStreams(4)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sfxIds = HashMap<Sfx, Int>()

    private var tts: TextToSpeech? = null
    var ttsOk by mutableStateOf(false)
    var ttsSpeaking by mutableStateOf(false)

    init {
        sfxIds[Sfx.Click] = pool.load(ctx, R.raw.sfx_click, 1)
        sfxIds[Sfx.Correct] = pool.load(ctx, R.raw.sfx_correct, 1)
        sfxIds[Sfx.Wrong] = pool.load(ctx, R.raw.sfx_wrong, 1)
        sfxIds[Sfx.Coin] = pool.load(ctx, R.raw.sfx_coin, 1)
        sfxIds[Sfx.Win] = pool.load(ctx, R.raw.sfx_win, 1)
        try {
            tts = TextToSpeech(ctx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val t = tts
                    val r = t?.setLanguage(Locale("fa", "IR")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    ttsOk = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                    t?.setPitchSafe(1.25f)
                    t?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { main.post { ttsSpeaking = true } }
                        override fun onDone(utteranceId: String?) { main.post { ttsSpeaking = false } }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) { main.post { ttsSpeaking = false } }
                    })
                }
            }
        } catch (e: Exception) { ttsOk = false }
    }

    private fun TextToSpeech.setPitchSafe(p: Float) { try { setPitch(p) } catch (_: Exception) {} }

    fun sfx(s: Sfx) {
        if (!store.sfxOn) return
        val id = sfxIds[s] ?: return
        pool.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun say(text: String) {
        if (!store.voiceOn || !ttsOk) return
        val b = Bundle()
        b.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, store.voiceVol.coerceIn(0f, 1f))
        try { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, b, "u" + System.nanoTime()) } catch (_: Exception) {}
    }

    fun stopSpeech() { try { tts?.stop() } catch (_: Exception) {}; ttsSpeaking = false }

    /** track: ۰ تا ۵ */
    fun playTrack(track: Int) {
        if (!store.musicOn) { stopMusicPlayer(pauseOnly = true); return }
        if (track == curTrack && mp != null) {
            mp?.setVolume(store.musicVol, store.musicVol)
            if (!pausedByApp && mp?.isPlaying == false) mp?.start()
            return
        }
        stopMusicPlayer(pauseOnly = false)
        try {
            val p = MediaPlayer.create(ctx, TrackRes[track.coerceIn(0, TrackRes.size - 1)])
            p?.isLooping = true
            p?.setVolume(store.musicVol, store.musicVol)
            if (!pausedByApp) p?.start()
            mp = p; curTrack = track
        } catch (_: Exception) { mp = null; curTrack = -1 }
    }

    fun applyVolume() { mp?.setVolume(store.musicVol, store.musicVol) }

    private fun stopMusicPlayer(pauseOnly: Boolean) {
        try {
            if (pauseOnly) { if (mp?.isPlaying == true) mp?.pause() }
            else { mp?.stop(); mp?.release(); mp = null; curTrack = -1 }
        } catch (_: Exception) {}
    }

    fun onAppPause() { pausedByApp = true; try { if (mp?.isPlaying == true) mp?.pause() } catch (_: Exception) {}; stopSpeech() }
    fun onAppResume() { pausedByApp = false; try { if (store.musicOn && mp?.isPlaying == false) mp?.start() } catch (_: Exception) {} }

    fun release() {
        stopMusicPlayer(pauseOnly = false)
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        try { pool.release() } catch (_: Exception) {}
    }
}
