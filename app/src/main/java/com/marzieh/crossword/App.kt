package com.marzieh.crossword

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

enum class Screen { Splash, Home, Difficulty, Themes, Levels, Game, Achievements, Settings, Music, Outfits, About }
enum class Mood { Idle, Happy, Wrong, Think, Cheer }
enum class Hint { Letter, Wrong, Text, Voice }

val DiffNames = listOf("", "آسان", "متوسط", "سخت")
fun faNum(n: Int): String = n.toString().map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

class GameState(val puzzle: Puzzle, val diff: Int, val level: Int) {
    private val total = puzzle.rows * puzzle.cols
    val letters = mutableStateListOf<Char>().also { l -> repeat(total) { l.add(EMPTY) } }
    val locked = mutableStateListOf<Boolean>().also { l -> repeat(total) { l.add(false) } }
    val solved = mutableStateListOf<Int>()
    val wrongWords = mutableStateListOf<Int>()
    var selR by mutableIntStateOf(-1)
    var selC by mutableIntStateOf(-1)
    var horizontal by mutableStateOf(true)
    var hints by mutableIntStateOf(0)
    var score by mutableIntStateOf(0)
    var seconds by mutableIntStateOf(0)
    var done by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var lastAction by mutableIntStateOf(0)
    var stars by mutableIntStateOf(0)
    var reward by mutableIntStateOf(0)
    var onSolved: (Placed) -> Unit = {}
    var onWrong: () -> Unit = {}
    var onWin: () -> Unit = {}

    val maxHints: Int get() = when (diff) { 1 -> 10; 2 -> 6; else -> 3 }
    fun idx(r: Int, c: Int) = r * puzzle.cols + c
    fun cur(): Placed? = if (selR < 0) null else puzzle.wordAt(selR, selC, horizontal) ?: puzzle.wordAt(selR, selC, !horizontal)

    fun tapCell(r: Int, c: Int) {
        if (!puzzle.isCell(r, c) || done) return
        val hw = puzzle.wordAt(r, c, true)
        val vw = puzzle.wordAt(r, c, false)
        if (r == selR && c == selC) {
            if (hw != null && vw != null) horizontal = !horizontal
        } else {
            selR = r; selC = c
            horizontal = when {
                horizontal && hw != null -> true
                !horizontal && vw != null -> false
                hw != null -> true
                else -> false
            }
        }
        lastAction++
    }

    fun selectWord(w: Placed) {
        horizontal = w.horizontal
        val first = w.cells().firstOrNull { letters[idx(it.first, it.second)] == EMPTY } ?: w.cell(0)
        selR = first.first; selC = first.second
        lastAction++
    }

    private fun step(dir: Int) {
        val w = cur() ?: return
        val i = w.cells().indexOf(Pair(selR, selC))
        val n = i + dir
        if (n in 0 until w.len) { val (r, c) = w.cell(n); selR = r; selC = c }
    }

    fun type(ch: Char) {
        if (selR < 0 || done) return
        val i = idx(selR, selC)
        if (!locked[i]) {
            letters[i] = ch
            clearWrongAt(selR, selC)
        }
        step(1)
        lastAction++
        evaluate()
    }

    fun backspace() {
        if (selR < 0 || done) return
        val i = idx(selR, selC)
        if (letters[i] != EMPTY && !locked[i]) {
            letters[i] = EMPTY
            clearWrongAt(selR, selC)
        } else {
            step(-1)
            val j = idx(selR, selC)
            if (!locked[j]) { letters[j] = EMPTY; clearWrongAt(selR, selC) }
        }
        lastAction++
    }

    private fun clearWrongAt(r: Int, c: Int) {
        puzzle.words.forEachIndexed { k, w -> if (w.contains(r, c)) wrongWords.remove(k) }
    }

    private fun filled(w: Placed) = w.cells().all { letters[idx(it.first, it.second)] != EMPTY }
    private fun correct(w: Placed) = w.cells().withIndex().all { (i, p) -> letters[idx(p.first, p.second)] == w.entry.answer[i] }

    fun evaluate() {
        puzzle.words.forEachIndexed { k, w ->
            if (k !in solved && filled(w)) {
                if (correct(w)) {
                    solved.add(k); wrongWords.remove(k)
                    score += 10 + w.len * 2
                    onSolved(w)
                } else if (k !in wrongWords) {
                    wrongWords.add(k)
                    onWrong()
                }
            }
        }
        if (!done && solved.size == puzzle.words.size) { done = true; onWin() }
    }

    /** true اگر راهنما اعمال شد */
    fun applyHint(h: Hint): Boolean {
        when (h) {
            Hint.Letter -> {
                if (selR < 0) return false
                val i = idx(selR, selC)
                if (locked[i] || letters[i] == puzzle.solution[selR][selC]) return false
                letters[i] = puzzle.solution[selR][selC]; locked[i] = true
                clearWrongAt(selR, selC)
                evaluate()
            }
            Hint.Wrong -> {
                var any = false
                for (r in 0 until puzzle.rows) for (c in 0 until puzzle.cols) {
                    val i = idx(r, c)
                    if (puzzle.isCell(r, c) && letters[i] != EMPTY && !locked[i] && letters[i] != puzzle.solution[r][c]) { letters[i] = EMPTY; any = true }
                }
                if (!any) return false
                wrongWords.clear()
            }
            else -> {}
        }
        hints++
        score = (score - 5).coerceAtLeast(0)
        lastAction++
        return true
    }

    fun serialize(): String {
        val head = "$diff,$level,${puzzle.rows},${puzzle.cols},$hints,$score,$seconds"
        val ws = puzzle.words.joinToString(";") { "${it.entry.id},${it.row},${it.col},${if (it.horizontal) 1 else 0}" }
        val ls = letters.joinToString("") { if (it == EMPTY) "." else it.toString() }
        val lk = locked.joinToString("") { if (it) "1" else "0" }
        return listOf(head, ws, ls, lk, solved.joinToString(",")).joinToString("~")
    }

    companion object {
        fun restore(s: String, bank: Bank): GameState? {
            try {
                val p = s.split("~")
                if (p.size < 5) return null
                val h = p[0].split(",").map { it.toInt() }
                val words = p[1].split(";").map {
                    val q = it.split(",")
                    Placed(bank.byId[q[0].toInt()] ?: return null, q[1].toInt(), q[2].toInt(), q[3] == "1")
                }
                val g = GameState(Puzzle(h[2], h[3], words), h[0], h[1])
                g.hints = h[4]; g.score = h[5]; g.seconds = h[6]
                p[2].forEachIndexed { i, ch -> if (i < g.letters.size) g.letters[i] = if (ch == '.') EMPTY else ch }
                p[3].forEachIndexed { i, ch -> if (i < g.locked.size) g.locked[i] = ch == '1' }
                if (p[4].isNotEmpty()) p[4].split(",").forEach { g.solved.add(it.toInt()) }
                return g
            } catch (e: Exception) { return null }
        }
    }
}

class App(val ctx: Context, val activity: ComponentActivity) {
    val store = Store(ctx)
    val audio = Audio(ctx, store)
    val bank = Bank(ctx)
    private val scope = CoroutineScope(Dispatchers.Main)

    var screen by mutableStateOf(Screen.Splash)
    var showExit by mutableStateOf(false)
    var night by mutableStateOf(false)
    var mood by mutableStateOf(Mood.Idle)
    var bubble by mutableStateOf<String?>(null)
    var talking by mutableStateOf(false)
    var game by mutableStateOf<GameState?>(null)
    var toast by mutableStateOf<String?>(null)
    var pendingDiff by mutableIntStateOf(1)
    var greeted = false
    private var talkJob: Job? = null
    private var toastJob: Job? = null
    private var lastLine = ""

    val pal: Pal get() = makePal(store.themeIdx, night)

    init {
        refreshNight()
        val s = store.saved
        if (s.isNotEmpty() && GameState.restore(s, bank) == null) store.saved = ""
    }

    fun refreshNight() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        night = if (store.autoDay) (h >= 19 || h < 6) else store.manualNight
    }

    fun pick(list: List<String>): String {
        var s = list.random()
        if (list.size > 1) while (s == lastLine) s = list.random()
        lastLine = s
        return s
    }

    fun say(text: String, m: Mood = Mood.Idle, speak: Boolean = true) {
        bubble = text
        mood = m
        if (speak) audio.say(text)
        talkJob?.cancel()
        talkJob = scope.launch {
            talking = true
            delay((1400 + text.length * 70).toLong())
            talking = false
            delay(1200)
            if (!talking) { mood = Mood.Idle }
            delay(2500)
            if (!talking) bubble = null
        }
    }

    fun repeatLine() {
        val b = bubble
        say(b ?: pick(Lines.greetBack), mood)
    }

    fun showToast(t: String) {
        toast = t
        toastJob?.cancel()
        toastJob = scope.launch { delay(2800); toast = null }
    }

    fun go(s: Screen) { screen = s; showExit = false }

    // ---------- موسیقی ----------
    fun refreshMusic() {
        val st = store
        val track = when (val ch = st.musicChoice) {
            in 1..6 -> ch - 1
            7 -> if (randomTrack in 0..5) randomTrack else Random.nextInt(6).also { randomTrack = it }
            else -> autoTrack()
        }
        audio.playTrack(track)
    }
    private var randomTrack = -1
    fun reshuffleRandom() { randomTrack = Random.nextInt(6) }
    private fun autoTrack(): Int {
        val g = game
        val inGame = screen == Screen.Game && g != null
        if (night || store.themeIdx == 7) return 5
        if (inGame && g!!.diff == 3) return 4
        return when (store.themeIdx) { 0 -> 0; 1 -> 2; 2 -> 3; 3 -> 1; 4 -> 0; 5 -> 2; else -> 1 }
    }

    // ---------- بازی ----------
    private fun params(diff: Int, level: Int): Triple<Int, Int, Int> = when (diff) {
        1 -> Triple(7, minOf(5 + level / 10, 7), 4)
        2 -> Triple(9, minOf(7 + level / 8, 10), 5)
        else -> Triple(11, minOf(9 + level / 6, 13), 7)
    }

    fun newGame(diff: Int, level: Int) {
        val old = game
        if (old != null && !old.done && old.solved.isNotEmpty()) store.streak = 0
        val rnd = Random(System.nanoTime())
        val (size, target, minW) = params(diff, level)
        var puzzle: Puzzle? = null
        var resetNote = false
        for (t in 0 until 8) {
            val (pool, reset) = bank.pickPool(diff, rnd)
            if (reset) resetNote = true
            puzzle = Generator.generate(pool, size, target, minW, rnd)
            if (puzzle != null) break
        }
        if (puzzle == null) { showToast("ساخت جدول ممکن نشد؛ دوباره امتحان کن"); return }
        bank.markUsed(puzzle.words.map { it.entry.id }, diff)
        store.difficulty = diff
        store.playedMask = store.playedMask or (1 shl diff)
        attach(GameState(puzzle, diff, level))
        if (resetNote) showToast("همه‌ی سؤال‌های سطح ${DiffNames[diff]} استفاده شده بود؛ چرخه‌ی تازه‌ای شروع شد 🔄")
        go(Screen.Game)
        refreshMusic()
        say(pick(Lines.start), Mood.Happy)
        saveGame()
    }

    private fun attach(g: GameState) {
        game = g
        g.onSolved = { w ->
            audio.sfx(Sfx.Correct)
            store.solvedWords = store.solvedWords + 1
            store.coins = store.coins + 1
            store.score = store.score + 10 + w.len * 2
            say(pick(Lines.correct), Mood.Happy)
            saveGame()
        }
        g.onWrong = { audio.sfx(Sfx.Wrong); say(pick(Lines.wrong), Mood.Wrong) }
        g.onWin = { finish(g) }
    }

    fun continueGame() {
        val g = GameState.restore(store.saved, bank)
        if (g == null) { store.saved = ""; showToast("بازی ذخیره‌شده‌ای پیدا نشد"); return }
        attach(g)
        go(Screen.Game)
        refreshMusic()
        say("خوش برگشتی! ادامه بدیم؟", Mood.Happy)
    }

    val hasSaved: Boolean get() = store.saved.isNotEmpty()

    fun saveGame() {
        val g = game ?: return
        store.saved = if (g.done) "" else g.serialize()
    }

    private fun finish(g: GameState) {
        val par = g.puzzle.words.size * 45
        val stars = if (g.hints == 0) 3 else if (g.hints <= 2) 2 else 1
        var bonus = 50 + (if (g.hints == 0) 30 else 0) + maxOf(0, (par - g.seconds) / 2)
        var coins = 10 + stars * 5
        store.streak = store.streak + 1
        if (store.streak > store.bestStreak) store.bestStreak = store.streak
        if (store.streak % 3 == 0) { coins += 25; bonus += 40 }
        g.stars = stars
        g.score += bonus
        g.reward = coins
        store.score = store.score + bonus
        store.stars = store.stars + stars
        store.coins = store.coins + coins
        store.solvedPuzzles = store.solvedPuzzles + 1
        if (g.hints == 0) store.noHintWins = store.noHintWins + 1
        if (stars == 3) store.threeStarWins = store.threeStarWins + 1
        val bt = store.bestTime(g.diff)
        if (bt == 0 || g.seconds < bt) store.setBestTime(g.diff, g.seconds)
        if (g.level >= store.unlocked(g.diff) && g.level < 30) store.setUnlocked(g.diff, g.level + 1)
        store.saved = ""
        audio.sfx(Sfx.Win)
        say(pick(Lines.levelDone), Mood.Cheer)
    }

    fun useHint(h: Hint) {
        val g = game ?: return
        if (g.done) return
        val cost = when (h) { Hint.Letter -> 5; Hint.Wrong -> 3; Hint.Text -> 4; Hint.Voice -> 2 }
        if (g.hints >= g.maxHints) { showToast("سقف راهنمای این جدول تمام شد"); return }
        if (store.coins < cost) { showToast("سکه‌ی کافی نداری 🪙"); return }
        when (h) {
            Hint.Letter -> {
                if (g.selR < 0) { showToast("اول یک خانه را انتخاب کن"); return }
                if (!g.applyHint(h)) { showToast("این خانه همین حالا درست است"); return }
            }
            Hint.Wrong -> if (!g.applyHint(h)) { showToast("حرف اشتباهی وجود ندارد 👍"); return }
            Hint.Text -> {
                val w = g.cur() ?: run { showToast("اول یک سؤال را انتخاب کن"); return }
                g.hints++; g.score = (g.score - 5).coerceAtLeast(0)
                say("دسته: ${w.entry.cat} — حرف اول: «${w.entry.answer[0]}» — ${faNum(w.len)} حرفی", Mood.Think)
            }
            Hint.Voice -> {
                val w = g.cur() ?: run { showToast("اول یک سؤال را انتخاب کن"); return }
                g.hints++; g.score = (g.score - 5).coerceAtLeast(0)
                say(w.entry.clue.replace("...", "چیست"), Mood.Think)
            }
        }
        store.coins = store.coins - cost
        audio.sfx(Sfx.Coin)
        saveGame()
    }

    // ---------- ناوبری ----------
    fun onBack() {
        if (showExit) { showExit = false; return }
        val g = game
        when (screen) {
            Screen.Splash -> {}
            Screen.Home -> showExit = true
            Screen.Game -> if (g != null && g.done) { go(Screen.Home) } else if (g != null) { g.paused = !g.paused; saveGame() } else go(Screen.Home)
            Screen.Levels -> go(Screen.Difficulty)
            Screen.Music, Screen.Outfits -> go(Screen.Settings)
            else -> go(Screen.Home)
        }
    }

    fun confirmExit() {
        saveGame()
        audio.release()
        activity.finishAffinity()
    }

    fun onPause() { saveGame(); audio.onAppPause() }
    fun onResume() { audio.onAppResume(); refreshNight() }
    fun destroy() { audio.release() }

    // ---------- دستاوردها ----------
    class Ach(val title: String, val desc: String, val medal: String, val value: Int, val target: Int)

    fun achievements(): List<Ach> = listOf(
        Ach("اولین جدول", "یک جدول را کامل کن", "🥉", store.solvedPuzzles, 1),
        Ach("جدول‌باز ماهر", "۱۰ جدول را کامل کن", "🥈", store.solvedPuzzles, 10),
        Ach("استاد جدول", "۵۰ جدول را کامل کن", "🥇", store.solvedPuzzles, 50),
        Ach("کلمه‌یاب", "۱۰۰ کلمه را درست حل کن", "📚", store.solvedWords, 100),
        Ach("بدون کمک", "۵ جدول را بدون راهنما حل کن", "🧠", store.noHintWins, 5),
        Ach("ستاره‌باران", "۱۰ بار سه‌ستاره بگیر", "🌟", store.threeStarWins, 10),
        Ach("پس‌انداز", "۳۰۰ سکه جمع کن", "🪙", store.coins, 300),
        Ach("پشت‌سرهم", "۵ جدول پیاپی حل کن", "🔥", store.bestStreak, 5),
        Ach("هر سه سطح", "هر سه سطح دشواری را بازی کن", "🎯", Integer.bitCount(store.playedMask), 3),
        Ach("کلکسیونر تم", "۴ تم را باز کن", "🎨", store.ownedThemes.split(",").size, 4)
    )
}
