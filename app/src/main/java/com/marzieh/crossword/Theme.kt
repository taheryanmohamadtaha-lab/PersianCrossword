package com.marzieh.crossword

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

class GameTheme(
    val name: String, val emoji: String, val top: Color, val bottom: Color,
    val primary: Color, val accent: Color, val deco: List<String>, val cost: Int, val alwaysDark: Boolean = false
)

val Themes = listOf(
    GameTheme("فانتزی رنگارنگ", "🦄", Color(0xFFFFD6F6), Color(0xFFB8C0FF), Color(0xFFE5408F), Color(0xFFFFC857), listOf("🦄", "🌈", "⭐", "🎈", "💖", "🍭"), 0),
    GameTheme("جنگل جادویی", "🌳", Color(0xFFD7F5C4), Color(0xFF6CCB8A), Color(0xFF2E9E5B), Color(0xFFFFB74D), listOf("🌳", "🦊", "🐰", "🍄", "🦋", "🌿"), 0),
    GameTheme("فضایی", "🪐", Color(0xFF1B1F5E), Color(0xFF5B2A86), Color(0xFF7C4DFF), Color(0xFFFFD54F), listOf("🪐", "🚀", "⭐", "🌙", "🛸", "☄️"), 60, true),
    GameTheme("دریایی", "🐠", Color(0xFFBDF0FF), Color(0xFF3AA8E0), Color(0xFF0288D1), Color(0xFFFF8A65), listOf("🐠", "🐙", "🐳", "🦀", "🐚", "💧"), 80),
    GameTheme("شیرینی و شکلات", "🍫", Color(0xFFFFE0C2), Color(0xFFF4A6A6), Color(0xFFA9581C), Color(0xFFFF6F91), listOf("🍫", "🍩", "🧁", "🍬", "🍪", "🍰"), 100),
    GameTheme("آسمانی", "🌈", Color(0xFFD6F0FF), Color(0xFFFDE2F3), Color(0xFF4F8CFF), Color(0xFFFF9AA2), listOf("☁️", "🌈", "🕊️", "☀️", "🎈"), 120),
    GameTheme("مینیمال مدرن", "◻️", Color(0xFFF7F7F9), Color(0xFFE6E8EE), Color(0xFF37474F), Color(0xFF26A69A), emptyList(), 50),
    GameTheme("شب", "🌙", Color(0xFF0D1030), Color(0xFF1C2147), Color(0xFF5C6BC0), Color(0xFFFFD54F), listOf("🌙", "⭐", "✨"), 70, true)
)

class Pal(
    val top: Color, val bottom: Color, val primary: Color, val accent: Color, val card: Color,
    val text: Color, val cell: Color, val block: Color, val deco: List<String>, val dark: Boolean, val shape: Shape, val small: Shape
)

fun makePal(idx: Int, night: Boolean): Pal {
    val t = Themes[idx.coerceIn(0, Themes.size - 1)]
    val dark = night || t.alwaysDark
    val top = if (night && !t.alwaysDark) lerp(t.top, Color(0xFF0B0E26), 0.8f) else t.top
    val bottom = if (night && !t.alwaysDark) lerp(t.bottom, Color(0xFF0B0E26), 0.72f) else t.bottom
    val primary = if (night && !t.alwaysDark) lerp(t.primary, Color(0xFF3C4A9A), 0.35f) else t.primary
    val shape: Shape = when (idx) { 6 -> RoundedCornerShape(8.dp); 2 -> CutCornerShape(14.dp); 4 -> RoundedCornerShape(30.dp); else -> RoundedCornerShape(22.dp) }
    val small: Shape = if (idx == 2) CutCornerShape(6.dp) else RoundedCornerShape(if (idx == 6) 4.dp else 10.dp)
    return Pal(
        top, bottom, primary, t.accent,
        card = if (dark) Color(0x44FFFFFF) else Color(0xDDFFFFFF),
        text = if (dark) Color(0xFFF4F4FF) else Color(0xFF2B2B3A),
        cell = if (dark) Color(0xFF2A2F5A) else Color.White,
        block = if (dark) Color(0x22000000) else Color(0x22000000),
        deco = if (idx == 6) emptyList() else t.deco, dark = dark, shape = shape, small = small
    )
}

val Accessories = listOf(
    Triple("بدون لوازم", "—", 0),
    Triple("تاج", "👑", 80),
    Triple("پاپیون", "🎀", 40),
    Triple("عینک آفتابی", "🕶️", 60),
    Triple("کلاه", "🎩", 70),
    Triple("گل سر", "🌸", 30),
    Triple("پروانه", "🦋", 50)
)

object Lines {
    val start = listOf(
        "سلام دوست من! آماده‌ای با هم کلی کلمه جدید یاد بگیریم؟",
        "سلام سلام! امروز چه جدول‌هایی حل می‌کنیم؟",
        "به به، چه خوب که اومدی! بزن بریم سراغ کلمه‌ها!"
    )
    val correct = listOf(
        "آفرین! چه باهوشی! عالی جواب دادی!",
        "دقیقاً همینه! دمت گرم!",
        "وای چه سریع! درست بود!",
        "ایول! یه کلمه‌ی دیگه حل شد!"
    )
    val wrong = listOf(
        "اوه! این یکی درست نبود، دوباره امتحان کن!",
        "هوم، فکر کنم یه حرفش اشتباهه. یه بار دیگه نگاه کن!",
        "نگران نباش، اشتباه کردن جزو بازیه! دوباره تلاش کن."
    )
    val think = listOf(
        "یه کم فکر کن، مطمئنم جوابش رو پیدا می‌کنی!",
        "به حرف‌های تقاطعی نگاه کن، شاید کمکت کنن!",
        "آروم و با دقت! تو از پسش برمیای."
    )
    val levelDone = listOf(
        "هورااا! تو این مرحله رو هم با موفقیت تموم کردی!",
        "عالی بود! جدول کامل شد!",
        "تبریک! قهرمان جدول‌ها شدی!"
    )
    val star = listOf("تبریک می‌گم! یه ستاره دیگه به دست آوردی!", "ستاره‌ی تازه مبارک!")
    val idle = listOf(
        "هنوز اینجام! بیا با هم این جدول رو حل کنیم!",
        "کجایی؟ جدول منتظرته!",
        "اگه گیر کردی، از راهنما کمک بگیر!"
    )
    val greetBack = listOf("بریم یه جدول حل کنیم؟", "هر روز یه جدول، هر روز یه کلمه‌ی تازه!", "من آماده‌ام، تو چی؟")
}
