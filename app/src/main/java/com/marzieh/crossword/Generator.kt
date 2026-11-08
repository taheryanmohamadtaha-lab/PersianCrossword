package com.marzieh.crossword

import kotlin.random.Random

/** سازنده‌ی جدول: هر بار جدول کاملاً جدید با تقاطع واقعی می‌سازد و پیش از نمایش اعتبارسنجی می‌کند. */
object Generator {
    private fun empty(g: Array<CharArray>, r: Int, c: Int) =
        r < 0 || c < 0 || r >= g.size || c >= g[0].size || g[r][c] == EMPTY

    private fun canPlace(g: Array<CharArray>, dir: Array<IntArray>, a: String, r: Int, c: Int, h: Boolean): Int {
        val n = g.size
        val er = if (h) r else r + a.length - 1
        val ec = if (h) c + a.length - 1 else c
        if (r < 0 || c < 0 || er >= n || ec >= n) return -1
        if (h) {
            if (!empty(g, r, c - 1) || !empty(g, r, ec + 1)) return -1
        } else {
            if (!empty(g, r - 1, c) || !empty(g, er + 1, c)) return -1
        }
        val bit = if (h) 1 else 2
        var cross = 0
        for (i in a.indices) {
            val rr = if (h) r else r + i
            val cc = if (h) c + i else c
            if (g[rr][cc] != EMPTY) {
                if (g[rr][cc] != a[i]) return -1
                if ((dir[rr][cc] and bit) != 0) return -1
                cross++
            } else {
                if (h) {
                    if (!empty(g, rr - 1, cc) || !empty(g, rr + 1, cc)) return -1
                } else {
                    if (!empty(g, rr, cc - 1) || !empty(g, rr, cc + 1)) return -1
                }
            }
        }
        return if (cross >= 1 && cross < a.length) cross else -1
    }

    private fun put(g: Array<CharArray>, dir: Array<IntArray>, p: Placed) {
        for (i in 0 until p.len) {
            val (r, c) = p.cell(i)
            g[r][c] = p.entry.answer[i]
            dir[r][c] = dir[r][c] or (if (p.horizontal) 1 else 2)
        }
    }

    fun generate(pool: List<WordEntry>, size: Int, target: Int, minWords: Int, rnd: Random): Puzzle? {
        val cand0 = pool.filter { it.answer.length in 2..size }
        if (cand0.size < minWords) return null
        var best: List<Placed> = emptyList()
        var bestScore = -1.0
        for (attempt in 0 until 60) {
            val g = Array(size) { CharArray(size) { EMPTY } }
            val dir = Array(size) { IntArray(size) }
            val cand = cand0.shuffled(rnd)
            val first = cand.take(6).maxByOrNull { it.answer.length } ?: return null
            val r0 = size / 2
            val c0 = rnd.nextInt(0, size - first.answer.length + 1)
            val placed = ArrayList<Placed>()
            val p0 = Placed(first, r0, c0, true)
            put(g, dir, p0); placed.add(p0)
            // هر بار پرتقاطع‌ترین جای ممکن را برمی‌گزینیم تا جدول فشرده و پر شود
            while (placed.size < target) {
                var bestOpt: Placed? = null
                var bestCross = 0
                for (w in cand) {
                    if (placed.any { it.entry.answer == w.answer }) continue
                    for (p in placed) {
                        for (i in w.answer.indices) for (j in 0 until p.len) {
                            if (w.answer[i] != p.entry.answer[j]) continue
                            val h = !p.horizontal
                            val (pr, pc) = p.cell(j)
                            val r = if (h) pr else pr - i
                            val c = if (h) pc - i else pc
                            val x = canPlace(g, dir, w.answer, r, c, h)
                            if (x > 0 && (x > bestCross || (x == bestCross && rnd.nextInt(3) == 0))) {
                                bestCross = x; bestOpt = Placed(w, r, c, h)
                            }
                        }
                    }
                }
                val o = bestOpt ?: break
                put(g, dir, o); placed.add(o)
            }
            // امتیاز: تعداد کلمه و تراکم خانه‌های پر نسبت به مساحت جدول
            var minR = Int.MAX_VALUE; var maxR = 0; var minC = Int.MAX_VALUE; var maxC = 0; var cells = 0
            for (r in 0 until size) for (c in 0 until size) if (g[r][c] != EMPTY) {
                cells++; minR = minOf(minR, r); maxR = maxOf(maxR, r); minC = minOf(minC, c); maxC = maxOf(maxC, c)
            }
            val area = (maxR - minR + 1) * (maxC - minC + 1)
            val score = placed.size * 10.0 + 100.0 * cells / area
            if (placed.size >= minWords && score > bestScore) { bestScore = score; best = placed.toList() }
        }
        if (best.size < minWords) return null
        var minR = Int.MAX_VALUE; var maxR = 0; var minC = Int.MAX_VALUE; var maxC = 0
        for (w in best) for ((r, c) in w.cells()) {
            minR = minOf(minR, r); maxR = maxOf(maxR, r); minC = minOf(minC, c); maxC = maxOf(maxC, c)
        }
        val shifted = best.map { Placed(it.entry, it.row - minR, it.col - minC, it.horizontal) }
        val puzzle = Puzzle(maxR - minR + 1, maxC - minC + 1, shifted)
        return if (valid(puzzle)) puzzle else null
    }

    /** اعتبارسنجی نهایی: هر دنباله‌ی ≥۲ حرفی افقی/عمودی باید دقیقاً یک کلمه‌ی ثبت‌شده باشد و حروف همخوان باشند. */
    fun valid(p: Puzzle): Boolean {
        for (w in p.words) for (i in 0 until w.len) {
            val (r, c) = w.cell(i)
            if (p.solution[r][c] != w.entry.answer[i]) return false
        }
        for (r in 0 until p.rows) {
            var c = 0
            while (c < p.cols) {
                if (p.isCell(r, c)) {
                    var e = c
                    while (e + 1 < p.cols && p.isCell(r, e + 1)) e++
                    if (e > c && p.words.none { it.horizontal && it.row == r && it.col == c && it.len == e - c + 1 }) return false
                    c = e + 1
                } else c++
            }
        }
        for (c in 0 until p.cols) {
            var r = 0
            while (r < p.rows) {
                if (p.isCell(r, c)) {
                    var e = r
                    while (e + 1 < p.rows && p.isCell(e + 1, c)) e++
                    if (e > r && p.words.none { !it.horizontal && it.col == c && it.row == r && it.len == e - r + 1 }) return false
                    r = e + 1
                } else r++
            }
        }
        return true
    }
}
