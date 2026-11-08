package com.marzieh.crossword

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ پایه‌ها

@Composable
fun AppRoot(app: App) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LaunchedEffect(Unit) { while (true) { delay(60000); app.refreshNight() } }
        LaunchedEffect(app.screen, app.store.themeIdx, app.night, app.store.musicOn, app.store.musicChoice, app.game?.diff) { app.refreshMusic() }
        LaunchedEffect(app.store.musicVol) { app.audio.applyVolume() }
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = app.screen,
                transitionSpec = { (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 8 }) togetherWith fadeOut(tween(200)) },
                label = "nav"
            ) { s ->
                when (s) {
                    Screen.Splash -> SplashScreen(app)
                    Screen.Home -> HomeScreen(app)
                    Screen.Difficulty -> DifficultyScreen(app)
                    Screen.Themes -> ThemesScreen(app)
                    Screen.Levels -> LevelsScreen(app)
                    Screen.Game -> GameScreen(app)
                    Screen.Achievements -> AchievementsScreen(app)
                    Screen.Settings -> SettingsScreen(app)
                    Screen.Music -> MusicScreen(app)
                    Screen.Outfits -> OutfitsScreen(app)
                    Screen.About -> AboutScreen(app)
                }
            }
            val t = app.toast
            if (t != null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 20.dp, end = 20.dp)) {
                    Panel(app) { Text(t, color = app.pal.text, fontSize = 15.sp, textAlign = TextAlign.Center) }
                }
            }
            if (app.showExit) ExitDialog(app)
        }
    }
}

@Composable
fun Panel(app: App, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = app.pal
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (p.dark) Color(0xEE22264D) else Color(0xF2FFFFFF)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, content = content
    )
}

@Composable
fun Btn(app: App, text: String, modifier: Modifier = Modifier, color: Color? = null, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    val p = app.pal
    val c0 = color ?: p.primary
    val c = if (enabled) c0 else c0.copy(alpha = 0.4f)
    Box(
        modifier.clip(if (compact) p.small else p.shape)
            .background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.28f), c)))
            .clickable(enabled = enabled) { app.audio.sfx(Sfx.Click); onClick() }
            .padding(horizontal = if (compact) 10.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Decor(emojis: List<String>) {
    if (emojis.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val inf = rememberInfiniteTransition(label = "decor")
        for (i in 0 until 10) {
            val e = emojis[i % emojis.size]
            val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6000 + i * 700, easing = LinearEasing), RepeatMode.Reverse), label = "d$i")
            val fx = ((i * 37) % 100) / 100f
            val fy = 0.04f + 0.8f * ((((i * 53) % 100) / 100f) * 0.4f + t * 0.6f)
            Text(e, fontSize = (22 + (i % 3) * 8).sp, modifier = Modifier.offset(x = w * fx, y = h * fy).alpha(0.45f))
        }
    }
}

@Composable
fun Page(app: App, title: String?, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val p = app.pal
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.top, p.bottom)))) {
        Decor(p.deco)
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            if (title != null || onBack != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) { Btn(app, "→ بازگشت", compact = true) { onBack() }; Spacer(Modifier.width(8.dp)) }
                    if (title != null) Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    if (onBack != null) Btn(app, "🏠 منو", compact = true, color = Color(0xFF78909C)) { app.go(Screen.Home) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally, content = content
            )
        }
    }
}

@Composable
fun Bubble(app: App, modifier: Modifier = Modifier) {
    val b = app.bubble
    Box(modifier.heightIn(min = 60.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = b != null, enter = fadeIn() + scaleIn(), exit = fadeOut()) {
            Panel(app) { Text(b ?: "", color = app.pal.text, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 4) }
        }
    }
}

@Composable
fun ExitDialog(app: App) {
    Dialog(onDismissRequest = { app.showExit = false }) {
        Panel(app) {
            Text("🚪", fontSize = 36.sp)
            Text("آیا مطمئنی می‌خواهی از بازی خارج شوی؟", color = app.pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Btn(app, "بله، خروج", Modifier.fillMaxWidth(), color = Color(0xFFE57373)) { app.confirmExit() }
            Spacer(Modifier.height(8.dp))
            Btn(app, "بازگشت به بازی", Modifier.fillMaxWidth()) { app.showExit = false }
        }
    }
}

// ------------------------------------------------------------------ صفحه‌ها

@Composable
fun SplashScreen(app: App) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(2400); app.go(Screen.Home) }
    val sc by animateFloatAsState(if (shown) 1f else 0.3f, tween(900, easing = FastOutSlowInEasing), label = "sc")
    val al by animateFloatAsState(if (shown) 1f else 0f, tween(900), label = "al")
    val p = app.pal
    Page(app, null, null) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.graphicsLayerCompat(sc, al)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✨ 🔤 ✨", fontSize = 34.sp)
                Text("جدول جادویی فارسی", color = p.primary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("Persian Crossword Adventure", color = p.text, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                CharacterView(app, 250.dp)
                Spacer(Modifier.height(10.dp))
                Text("طراحی و سازنده: مرضیه برقی", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun Modifier.graphicsLayerCompat(scale: Float, alpha: Float): Modifier =
    this.then(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha })

@Composable
fun HomeScreen(app: App) {
    val p = app.pal
    LaunchedEffect(Unit) {
        if (!app.greeted) { app.greeted = true; delay(600); app.say(app.pick(Lines.start), Mood.Happy) }
    }
    Page(app, null, null) {
        Text("جدول جادویی فارسی", color = p.primary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Bubble(app, Modifier.fillMaxWidth())
        CharacterView(app, 210.dp)
        Spacer(Modifier.height(6.dp))
        val m = Modifier.fillMaxWidth(0.82f)
        if (app.hasSaved) { Btn(app, "▶️ ادامه‌ی آخرین مرحله", m, color = Color(0xFF43A047)) { app.continueGame() }; Spacer(Modifier.height(8.dp)) }
        Btn(app, "🎮 بازی جدید", m) { app.go(Screen.Difficulty) }
        Spacer(Modifier.height(8.dp))
        Btn(app, "🎨 تم‌ها", m, color = p.accent) { app.go(Screen.Themes) }
        Spacer(Modifier.height(8.dp))
        Btn(app, "🏆 دستاوردها", m, color = p.accent) { app.go(Screen.Achievements) }
        Spacer(Modifier.height(8.dp))
        Btn(app, "⚙️ تنظیمات", m, color = Color(0xFF78909C)) { app.go(Screen.Settings) }
        Spacer(Modifier.height(8.dp))
        Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(app, "ℹ️ درباره", Modifier.weight(1f), color = Color(0xFF78909C)) { app.go(Screen.About) }
            Btn(app, "🚪 خروج", Modifier.weight(1f), color = Color(0xFFE57373)) { app.showExit = true }
        }
        Spacer(Modifier.height(12.dp))
        Text("طراحی و سازنده: مرضیه برقی", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("🪙 ${faNum(app.store.coins)}   ⭐ ${faNum(app.store.stars)}", color = p.text, fontSize = 14.sp)
    }
}

@Composable
fun DifficultyScreen(app: App) {
    val p = app.pal
    Page(app, "انتخاب سطح دشواری", { app.go(Screen.Home) }) {
        val info = listOf(
            Triple("🌱 آسان", "جدول کوچک، کلمات روزمره، راهنمای بیشتر (تا ۱۰ راهنما)", Color(0xFF66BB6A)),
            Triple("🌟 متوسط", "جدول بزرگ‌تر، ضرب‌المثل و واژگان متنوع (تا ۶ راهنما)", Color(0xFFFFA726)),
            Triple("🔥 سخت", "جدول پیچیده، سؤال‌های علمی و ادبی و تاریخی (تا ۳ راهنما)", Color(0xFFEF5350))
        )
        for (d in 1..3) {
            val (t, desc, c) = info[d - 1]
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.25f), c)))
                    .clickable { app.audio.sfx(Sfx.Click); app.store.difficulty = d; app.go(Screen.Levels) }
                    .padding(16.dp)
            ) {
                Text(t, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(desc, color = Color.White, fontSize = 14.sp)
                Text("سؤال‌های مصرف‌شده: ${faNum(app.bank.usedCount(d))} از ${faNum(app.bank.totalCount(d))}", color = Color.White, fontSize = 12.sp)
                Text("مرحله‌ی باز: ${faNum(app.store.unlocked(d))} از ${faNum(30)}", color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("هر زمان می‌توانی از منوی توقف، سطح را عوض کنی.", color = p.text, fontSize = 13.sp)
    }
}

@Composable
fun LevelsScreen(app: App) {
    val p = app.pal
    val d = app.store.difficulty
    val unl = app.store.unlocked(d)
    Page(app, "مرحله‌ها — سطح ${DiffNames[d]}", { app.go(Screen.Difficulty) }) {
        Text("پیشرفت: ${faNum(unl - 1)} از ${faNum(30)}", color = p.text, fontSize = 15.sp)
        Box(Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x33000000))) {
            Box(Modifier.fillMaxWidth(((unl - 1) / 30f).coerceIn(0f, 1f)).fillMaxHeight().background(p.accent))
        }
        for (row in 0 until 6) {
            Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until 5) {
                    val n = row * 5 + col + 1
                    val open = n <= unl
                    Box(
                        Modifier.size(54.dp).clip(p.small)
                            .background(if (open) (if (n < unl) Color(0xFF66BB6A) else p.primary) else Color(0x55000000))
                            .clickable(enabled = open) { app.audio.sfx(Sfx.Click); app.newGame(d, n) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (open) faNum(n) else "🔒", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("با هر بار شروع، جدول و سؤال‌های کاملاً تازه ساخته می‌شود.", color = p.text, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ThemesScreen(app: App) {
    val p = app.pal
    val st = app.store
    Page(app, "انتخاب تم", { app.go(Screen.Home) }) {
        Panel(app, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🌗 روز و شب خودکار", color = p.text, modifier = Modifier.weight(1f))
                Switch(st.autoDay, { st.autoDay = it; app.refreshNight() }, colors = SwitchDefaults.colors(checkedTrackColor = p.primary))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🌙 حالت شب (دستی)", color = p.text.copy(alpha = if (st.autoDay) 0.4f else 1f), modifier = Modifier.weight(1f))
                Switch(st.manualNight, { st.manualNight = it; app.refreshNight() }, enabled = !st.autoDay, colors = SwitchDefaults.colors(checkedTrackColor = p.primary))
            }
        }
        Text("🪙 ${faNum(st.coins)}", color = p.text, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
        Themes.chunked(2).forEachIndexed { ri, pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { ci, th ->
                    val i = ri * 2 + ci
                    val owned = st.ownsTheme(i) || th.cost == 0
                    val sel = st.themeIdx == i
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(th.top, th.bottom)))
                            .border(if (sel) 4.dp else 0.dp, if (sel) Color(0xFFFFD600) else Color.Transparent, RoundedCornerShape(18.dp))
                            .clickable {
                                app.audio.sfx(Sfx.Click)
                                if (owned) { st.themeIdx = i }
                                else if (st.coins >= th.cost) { st.coins -= th.cost; st.addTheme(i); st.themeIdx = i; app.audio.sfx(Sfx.Coin); app.showToast("تم «${th.name}» باز شد 🎉") }
                                else app.showToast("سکه‌ی کافی نداری 🪙")
                            }.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(th.emoji, fontSize = 36.sp)
                        val tc = if (th.alwaysDark) Color.White else Color(0xFF2B2B3A)
                        Text(th.name, color = tc, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text(if (sel) "✔ فعال" else if (owned) "انتخاب" else "🔒 ${faNum(th.cost)} سکه", color = tc, fontSize = 12.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AchievementsScreen(app: App) {
    val p = app.pal
    val st = app.store
    Page(app, "دستاوردها و رکوردها", { app.go(Screen.Home) }) {
        Panel(app, Modifier.fillMaxWidth()) {
            Text("📊 رکوردهای من", color = p.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("امتیاز کل: ${faNum(st.score)}   ⭐ ${faNum(st.stars)}   🪙 ${faNum(st.coins)}", color = p.text, fontSize = 14.sp)
            Text("جدول حل‌شده: ${faNum(st.solvedPuzzles)}   کلمه: ${faNum(st.solvedWords)}", color = p.text, fontSize = 14.sp)
            Text("بهترین رشته‌ی پیاپی: ${faNum(st.bestStreak)}", color = p.text, fontSize = 14.sp)
            fun tm(s: Int) = if (s == 0) "—" else "${faNum(s / 60)}:${faNum(s % 60).padStart(2, '۰')}"
            Text("سریع‌ترین زمان — آسان: ${tm(st.bestTime1)}   متوسط: ${tm(st.bestTime2)}   سخت: ${tm(st.bestTime3)}", color = p.text, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        for (a in app.achievements()) {
            val done = a.value >= a.target
            Spacer(Modifier.height(8.dp))
            Panel(app, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) a.medal else "🔒", fontSize = 32.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, color = p.text, fontWeight = FontWeight.Bold)
                        Text(a.desc, color = p.text, fontSize = 12.sp)
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33000000))) {
                            Box(Modifier.fillMaxWidth((a.value.toFloat() / a.target).coerceIn(0f, 1f)).fillMaxHeight().background(if (done) Color(0xFF66BB6A) else p.accent))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${faNum(minOf(a.value, a.target))}/${faNum(a.target)}", color = p.text, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(app: App) {
    val p = app.pal
    val st = app.store
    val sw = SwitchDefaults.colors(checkedTrackColor = p.primary)
    val sl = SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary)
    Page(app, "تنظیمات", { app.go(Screen.Home) }) {
        Panel(app, Modifier.fillMaxWidth()) {
            Text("🎵 موسیقی", color = p.text, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("پخش موسیقی", color = p.text, modifier = Modifier.weight(1f)); Switch(st.musicOn, { st.musicOn = it }, colors = sw)
            }
            Slider(st.musicVol, { st.musicVol = it }, colors = sl)
            Btn(app, "انتخاب موسیقی", Modifier.fillMaxWidth(), compact = true) { app.go(Screen.Music) }
        }
        Spacer(Modifier.height(8.dp))
        Panel(app, Modifier.fillMaxWidth()) {
            Text("🗣️ صدای شخصیت", color = p.text, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("گفتار شخصیت", color = p.text, modifier = Modifier.weight(1f)); Switch(st.voiceOn, { st.voiceOn = it; if (!it) app.audio.stopSpeech() }, colors = sw)
            }
            Slider(st.voiceVol, { st.voiceVol = it }, colors = sl)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("افکت‌های صوتی", color = p.text, modifier = Modifier.weight(1f)); Switch(st.sfxOn, { st.sfxOn = it }, colors = sw)
            }
            Text(
                if (app.audio.ttsOk) "✅ صدای فارسی روی این دستگاه آماده است."
                else "⚠️ صدای فارسی پیدا نشد. در تنظیمات گوشی ← تبدیل متن به گفتار، موتور Google را انتخاب و زبان فارسی را دانلود کن. تا آن موقع شخصیت با حباب نوشته حرف می‌زند.",
                color = p.text, fontSize = 12.sp
            )
            Btn(app, "🔊 امتحان صدا", Modifier.fillMaxWidth(), compact = true) { app.say(app.pick(Lines.start), Mood.Happy) }
        }
        Spacer(Modifier.height(8.dp))
        Panel(app, Modifier.fillMaxWidth()) {
            Text("🎨 ظاهر", color = p.text, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(app, "تم‌ها", Modifier.weight(1f), compact = true) { app.go(Screen.Themes) }
                Btn(app, "لباس شخصیت", Modifier.weight(1f), compact = true) { app.go(Screen.Outfits) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Panel(app, Modifier.fillMaxWidth()) {
            Text("📚 سابقه‌ی سؤال‌ها", color = p.text, fontWeight = FontWeight.Bold)
            for (d in 1..3) Text("${DiffNames[d]}: ${faNum(app.bank.usedCount(d))} از ${faNum(app.bank.totalCount(d))} مصرف شده", color = p.text, fontSize = 13.sp)
            Text("وقتی سؤال‌های یک سطح تمام شود، چرخه‌ی تازه خودکار شروع می‌شود و به تو خبر می‌دهیم.", color = p.text, fontSize = 12.sp)
        }
    }
}

@Composable
fun MusicScreen(app: App) {
    val p = app.pal
    val st = app.store
    Page(app, "انتخاب موسیقی", { app.go(Screen.Settings) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("پخش موسیقی", color = p.text, modifier = Modifier.weight(1f))
            Switch(st.musicOn, { st.musicOn = it }, colors = SwitchDefaults.colors(checkedTrackColor = p.primary))
        }
        Slider(st.musicVol, { st.musicVol = it }, colors = SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary))
        val opts = listOf(0 to "🎛️ خودکار (متناسب با تم و سطح)") + TrackNames.mapIndexed { i, n -> (i + 1) to "🎵 $n" } + listOf(7 to "🔀 تصادفی")
        for ((k, label) in opts) {
            Spacer(Modifier.height(6.dp))
            Btn(app, (if (st.musicChoice == k) "✔ " else "") + label, Modifier.fillMaxWidth(), color = if (st.musicChoice == k) Color(0xFF43A047) else p.primary) {
                st.musicChoice = k
                if (k == 7) app.reshuffleRandom()
                app.refreshMusic()
            }
        }
    }
}

@Composable
fun OutfitsScreen(app: App) {
    val p = app.pal
    val st = app.store
    Page(app, "لباس و لوازم شخصیت", { app.go(Screen.Settings) }) {
        CharacterView(app, 200.dp)
        Text("🪙 ${faNum(st.coins)}", color = p.text, fontSize = 16.sp)
        Accessories.forEachIndexed { i, (name, emoji, cost) ->
            val owned = st.ownsOutfit(i) || cost == 0
            Spacer(Modifier.height(6.dp))
            Panel(app, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (emoji == "—") "🙂" else emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(name, color = p.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Btn(app, if (st.outfit == i) "✔ در حال استفاده" else if (owned) "استفاده" else "${faNum(cost)} 🪙", compact = true,
                        color = if (st.outfit == i) Color(0xFF43A047) else null) {
                        if (owned) { st.outfit = i; app.say("چه بامزه شدم!", Mood.Happy) }
                        else if (st.coins >= cost) { st.coins -= cost; st.addOutfit(i); st.outfit = i; app.audio.sfx(Sfx.Coin); app.say("ممنون! خیلی قشنگه!", Mood.Cheer) }
                        else app.showToast("سکه‌ی کافی نداری 🪙")
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(app: App) {
    val p = app.pal
    Page(app, "درباره‌ی بازی", { app.go(Screen.Home) }) {
        CharacterView(app, 180.dp)
        Text("جدول جادویی فارسی", color = p.primary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Panel(app, Modifier.fillMaxWidth()) {
            Text("طراحی و سازنده: مرضیه برقی", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("جدول کلمات متقاطع فارسی با شخصیت سخنگو، هشت تم، سه سطح دشواری و بانک سؤال بدون تکرار. بازی کاملاً آفلاین است و پیشرفتت خودکار ذخیره می‌شود.", color = p.text, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("نسخه‌ی ۱٫۰", color = p.text, fontSize = 12.sp)
        }
    }
}
