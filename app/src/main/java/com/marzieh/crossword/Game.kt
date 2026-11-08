package com.marzieh.crossword

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

private val KeyRows = listOf("ضصثقفغعهخحجچ", "شسیبلاتنمکگ", "ظطزرذدپو")

fun clock(s: Int) = "${faNum(s / 60)}:${faNum(s % 60).padStart(2, '۰')}"

@Composable
fun GameScreen(app: App) {
    val g = app.game
    if (g == null) { app.go(Screen.Home); return }
    val p = app.pal
    var showClues by remember { mutableStateOf(false) }

    LaunchedEffect(g, g.paused, g.done) {
        while (!g.paused && !g.done) { delay(1000); g.seconds++ }
    }
    LaunchedEffect(g.lastAction, g.paused, g.done) {
        if (g.paused || g.done) return@LaunchedEffect
        delay(15000)
        app.say(app.pick(Lines.think), Mood.Think)
        delay(25000)
        app.say(app.pick(Lines.idle), Mood.Idle)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.top, p.bottom)))) {
        Decor(p.deco)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp)) {
            // نوار بالا
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(app, "→", compact = true, color = Color(0xFF78909C)) { app.saveGame(); app.go(Screen.Levels) }
                Btn(app, "⏸", compact = true) { g.paused = true; app.saveGame() }
                Column(Modifier.weight(1f)) {
                    Text("سطح ${DiffNames[g.diff]} — مرحله ${faNum(g.level)}", color = p.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("⏱ ${clock(g.seconds)}   ✅ ${faNum(g.solved.size)}/${faNum(g.puzzle.words.size)}", color = p.text, fontSize = 12.sp)
                }
                Text("⭐ ${faNum(g.score)}  🪙 ${faNum(app.store.coins)}", color = p.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            // شخصیت + حباب
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CharacterView(app, 86.dp)
                Spacer(Modifier.width(6.dp))
                Bubble(app, Modifier.weight(1f))
            }
            // سؤال فعلی
            val w = g.cur()
            Panel(app, Modifier.fillMaxWidth().clickable { showClues = true }) {
                if (w != null) {
                    Text(
                        "${faNum(w.number)} ${if (w.horizontal) "افقی" else "عمودی"} (${faNum(w.len)} حرفی): ${w.entry.clue}",
                        color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                } else Text("یک خانه را لمس کن تا سؤالش را ببینی", color = p.text, fontSize = 14.sp)
                Text("📋 فهرست همه‌ی سؤال‌ها", color = p.primary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            GridView(app, g)
            Spacer(Modifier.height(6.dp))
            // راهنماها
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Btn(app, "💡 حرف\n۵🪙", Modifier.weight(1f), compact = true, color = Color(0xFFFFA726)) { app.useHint(Hint.Letter) }
                Btn(app, "🧹 اشتباه‌ها\n۳🪙", Modifier.weight(1f), compact = true, color = Color(0xFF26A69A)) { app.useHint(Hint.Wrong) }
                Btn(app, "📖 متن\n۴🪙", Modifier.weight(1f), compact = true, color = Color(0xFF5C6BC0)) { app.useHint(Hint.Text) }
                Btn(app, "🔊 صوتی\n۲🪙", Modifier.weight(1f), compact = true, color = Color(0xFFAB47BC)) { app.useHint(Hint.Voice) }
            }
            Text("راهنمای استفاده‌شده: ${faNum(g.hints)} از ${faNum(g.maxHints)}", color = p.text, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(4.dp))
            // صفحه‌کلید فارسی (هم‌ترتیب با صفحه‌کلید استاندارد گوشی)
            KeyRows.forEachIndexed { ri, row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ri == 2) Spacer(Modifier.weight(1f))
                    row.forEach { ch -> Key(app, ch.toString(), Modifier.weight(1f)) { g.type(ch); app.saveGame() } }
                    if (ri == 2) Key(app, "⌫", Modifier.weight(3f)) { g.backspace(); app.saveGame() }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.weight(4f))
                listOf('آ', 'ژ', 'ئ', 'ء').forEach { ch -> Key(app, ch.toString(), Modifier.weight(1f)) { g.type(ch); app.saveGame() } }
                Spacer(Modifier.weight(4f))
            }
            Spacer(Modifier.height(20.dp))
        }

        if (showClues) ClueDialog(app, g) { showClues = false }
        if (g.paused && !g.done) PauseOverlay(app, g)
        if (g.done) WinOverlay(app, g)
    }
}

@Composable
fun Key(app: App, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = app.pal
    Box(
        modifier.height(54.dp).clip(p.small).background(p.cell).clickable { app.audio.sfx(Sfx.Click); onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = p.text, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GridView(app: App, g: GameState) {
    val p = app.pal
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cell: Dp = ((maxWidth - 16.dp) / g.puzzle.cols).coerceAtMost(46.dp)
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(p.card).padding(4.dp)) {
            for (r in 0 until g.puzzle.rows) {
                Row {
                    for (c in 0 until g.puzzle.cols) CellView(app, g, r, c, cell)
                }
            }
        }
    }
}

@Composable
fun CellView(app: App, g: GameState, r: Int, c: Int, size: Dp) {
    val p = app.pal
    val pz = g.puzzle
    if (!pz.isCell(r, c)) {
        Box(Modifier.size(size).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(p.block))
        return
    }
    val i = g.idx(r, c)
    val sel = r == g.selR && c == g.selC
    val inWord = g.cur()?.contains(r, c) == true
    val wrong = pz.words.withIndex().any { (k, w) -> k in g.wrongWords && w.contains(r, c) }
    val solved = pz.words.withIndex().any { (k, w) -> k in g.solved && w.contains(r, c) }
    val bg = when {
        sel -> p.accent
        wrong -> Color(0xFFFF8A80)
        inWord -> p.primary.copy(alpha = 0.40f)
        solved -> Color(0xFFB9F6CA)
        else -> p.cell
    }
    val onLight = sel || wrong || solved
    val num = pz.words.firstOrNull { it.row == r && it.col == c }?.number
    Box(
        Modifier.size(size).padding(1.dp).clip(RoundedCornerShape(6.dp)).background(bg)
            .clickable { g.tapCell(r, c) },
        contentAlignment = Alignment.Center
    ) {
        val ch = g.letters[i]
        if (ch != EMPTY) {
            Text(
                ch.toString(), fontSize = (size.value * 0.52f).sp, fontWeight = FontWeight.Bold,
                color = if (g.locked[i]) Color(0xFF1565C0) else if (onLight) Color(0xFF222233) else p.text
            )
        }
        if (num != null) {
            Text(
                faNum(num), fontSize = (size.value * 0.26f).sp,
                color = if (onLight || !p.dark) Color(0xFF555566) else Color(0xFFCCCCEE),
                modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun ClueDialog(app: App, g: GameState, onClose: () -> Unit) {
    val p = app.pal
    val hs = g.puzzle.words.withIndex().filter { it.value.horizontal }.sortedBy { it.value.number }
    val vs = g.puzzle.words.withIndex().filter { !it.value.horizontal }.sortedBy { it.value.number }
    Dialog(onDismissRequest = onClose) {
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (p.dark) Color(0xFF22264D) else Color.White).padding(12.dp)) {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item { Text("↔️ افقی", color = p.primary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
                items(hs) { iv -> ClueRow(app, g, iv.index, iv.value, onClose) }
                item { Spacer(Modifier.height(10.dp)); Text("↕️ عمودی", color = p.primary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
                items(vs) { iv -> ClueRow(app, g, iv.index, iv.value, onClose) }
            }
        }
    }
}

@Composable
fun ClueRow(app: App, g: GameState, k: Int, w: Placed, onClose: () -> Unit) {
    val p = app.pal
    Row(
        Modifier.fillMaxWidth().clickable { g.selectWord(w); onClose() }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (k in g.solved) "✅" else "▫️", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text("${faNum(w.number)}. ${w.entry.clue} (${faNum(w.len)} حرفی)", color = p.text, fontSize = 14.sp)
    }
}

@Composable
fun Scrim(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xAA000000))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content) }
}

@Composable
fun PauseOverlay(app: App, g: GameState) {
    val p = app.pal
    Scrim {
        Panel(app, Modifier.fillMaxWidth()) {
            Text("⏸ بازی متوقف شد", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("پیشرفت: ${faNum(g.solved.size)} از ${faNum(g.puzzle.words.size)} کلمه", color = p.text, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            val m = Modifier.fillMaxWidth()
            Btn(app, "▶️ ادامه‌ی بازی", m, color = Color(0xFF43A047)) { g.paused = false }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🔄 جدول تازه (همین مرحله)", m) { app.newGame(g.diff, g.level) }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🎚️ تغییر سطح دشواری", m, color = p.accent) { app.saveGame(); app.go(Screen.Difficulty) }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🏠 ذخیره و رفتن به منو", m, color = Color(0xFF78909C)) { app.saveGame(); g.paused = false; app.go(Screen.Home) }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🚪 خروج از بازی", m, color = Color(0xFFE57373)) { app.showExit = true }
        }
    }
}

@Composable
fun WinOverlay(app: App, g: GameState) {
    val p = app.pal
    val inf = rememberInfiniteTransition(label = "win")
    val pulse by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Scrim {
        Panel(app, Modifier.fillMaxWidth()) {
            Text("🎉 آفرین! جدول کامل شد 🎉", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(("⭐".repeat(g.stars)) + ("☆".repeat(3 - g.stars)), fontSize = (38 * pulse).sp)
            Text("امتیاز: ${faNum(g.score)}   ⏱ ${clock(g.seconds)}", color = p.text, fontSize = 15.sp)
            Text("راهنمای استفاده‌شده: ${faNum(g.hints)}", color = p.text, fontSize = 13.sp)
            Text("🪙 پاداش: ${faNum(g.reward)} سکه", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (app.store.streak % 3 == 0 && app.store.streak > 0) Text("🔥 جایزه‌ی ویژه‌ی ۳ پیروزی پیاپی!", color = p.primary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            CharacterView(app, 110.dp)
            Spacer(Modifier.height(6.dp))
            val m = Modifier.fillMaxWidth()
            val next = if (g.level < 30) g.level + 1 else g.level
            Btn(app, if (g.level < 30) "➡️ مرحله‌ی بعد" else "🔁 دوباره", m, color = Color(0xFF43A047)) { app.newGame(g.diff, next) }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🔄 جدول تازه‌ی همین مرحله", m) { app.newGame(g.diff, g.level) }
            Spacer(Modifier.height(6.dp))
            Btn(app, "🏠 منوی اصلی", m, color = Color(0xFF78909C)) { app.go(Screen.Home) }
        }
    }
}
