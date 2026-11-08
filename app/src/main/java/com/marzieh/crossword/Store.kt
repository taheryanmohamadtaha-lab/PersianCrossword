package com.marzieh.crossword

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

class IntPref(private val sp: SharedPreferences, private val key: String, def: Int) {
    private val s = mutableIntStateOf(sp.getInt(key, def))
    operator fun getValue(t: Any?, p: KProperty<*>): Int = s.intValue
    operator fun setValue(t: Any?, p: KProperty<*>, v: Int) { s.intValue = v; sp.edit().putInt(key, v).apply() }
}

class FloatPref(private val sp: SharedPreferences, private val key: String, def: Float) {
    private val s = mutableFloatStateOf(sp.getFloat(key, def))
    operator fun getValue(t: Any?, p: KProperty<*>): Float = s.floatValue
    operator fun setValue(t: Any?, p: KProperty<*>, v: Float) { s.floatValue = v; sp.edit().putFloat(key, v).apply() }
}

class BoolPref(private val sp: SharedPreferences, private val key: String, def: Boolean) {
    private val s = mutableStateOf(sp.getBoolean(key, def))
    operator fun getValue(t: Any?, p: KProperty<*>): Boolean = s.value
    operator fun setValue(t: Any?, p: KProperty<*>, v: Boolean) { s.value = v; sp.edit().putBoolean(key, v).apply() }
}

class StrPref(private val sp: SharedPreferences, private val key: String, def: String) {
    private val s = mutableStateOf(sp.getString(key, def) ?: def)
    operator fun getValue(t: Any?, p: KProperty<*>): String = s.value
    operator fun setValue(t: Any?, p: KProperty<*>, v: String) { s.value = v; sp.edit().putString(key, v).apply() }
}

/** تمام تنظیمات و پیشرفت بازیکن؛ خودکار و بلافاصله ذخیره می‌شود. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cw_store", Context.MODE_PRIVATE)

    var coins by IntPref(sp, "coins", 40)
    var stars by IntPref(sp, "stars", 0)
    var score by IntPref(sp, "score", 0)

    var themeIdx by IntPref(sp, "theme", 0)
    var autoDay by BoolPref(sp, "autoDay", true)
    var manualNight by BoolPref(sp, "manualNight", false)
    var difficulty by IntPref(sp, "difficulty", 1)

    var musicOn by BoolPref(sp, "musicOn", true)
    var musicVol by FloatPref(sp, "musicVol", 0.5f)
    var musicChoice by IntPref(sp, "musicChoice", 0) // 0=خودکار، 1..6=آهنگ مشخص، 7=تصادفی
    var voiceOn by BoolPref(sp, "voiceOn", true)
    var voiceVol by FloatPref(sp, "voiceVol", 1f)
    var sfxOn by BoolPref(sp, "sfxOn", true)

    var unlocked1 by IntPref(sp, "unl1", 1)
    var unlocked2 by IntPref(sp, "unl2", 1)
    var unlocked3 by IntPref(sp, "unl3", 1)

    var solvedPuzzles by IntPref(sp, "solvedPuzzles", 0)
    var solvedWords by IntPref(sp, "solvedWords", 0)
    var noHintWins by IntPref(sp, "noHintWins", 0)
    var threeStarWins by IntPref(sp, "threeStarWins", 0)
    var streak by IntPref(sp, "streak", 0)
    var bestStreak by IntPref(sp, "bestStreak", 0)
    var playedMask by IntPref(sp, "playedMask", 0)
    var bestTime1 by IntPref(sp, "bt1", 0)
    var bestTime2 by IntPref(sp, "bt2", 0)
    var bestTime3 by IntPref(sp, "bt3", 0)

    var outfit by IntPref(sp, "outfit", 0)
    var ownedThemes by StrPref(sp, "ownedThemes", "0,1")
    var ownedOutfits by StrPref(sp, "ownedOutfits", "0")
    var saved by StrPref(sp, "savedGame", "")

    fun unlocked(d: Int) = when (d) { 1 -> unlocked1; 2 -> unlocked2; else -> unlocked3 }
    fun setUnlocked(d: Int, v: Int) { when (d) { 1 -> unlocked1 = v; 2 -> unlocked2 = v; else -> unlocked3 = v } }
    fun bestTime(d: Int) = when (d) { 1 -> bestTime1; 2 -> bestTime2; else -> bestTime3 }
    fun setBestTime(d: Int, v: Int) { when (d) { 1 -> bestTime1 = v; 2 -> bestTime2 = v; else -> bestTime3 = v } }

    fun ownsTheme(i: Int) = ownedThemes.split(",").contains(i.toString())
    fun ownsOutfit(i: Int) = ownedOutfits.split(",").contains(i.toString())
    fun addTheme(i: Int) { if (!ownsTheme(i)) ownedThemes = "$ownedThemes,$i" }
    fun addOutfit(i: Int) { if (!ownsOutfit(i)) ownedOutfits = "$ownedOutfits,$i" }
}
