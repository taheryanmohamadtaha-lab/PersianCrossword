package com.marzieh.crossword

import kotlin.random.Random

/** سازنده‌ی جدول: هر بار جدول کاملاً جدید با تقاطع واقعی می‌سازد و پیش از نمایش اعتبارسنجی می‌کند. */
object Generator {
    private fun empty(g: Array<CharArray>, r: Int, c: Int) =
        r < 0 || c < 0 || r >= g.size || c >= g[0].size || g[r][c] == EMPTY

    private fun canPlace(g: Array<CharArray>, dir: Array<IntArray>, a: String, r: Int, c: Int, h: Boolean): Boolean {
        val n = g.size
        val er = if (h) r else r + a.length - 1
        val ec = if (h) c + a.length - 1 else c
        if (r < 0 || c < 0 || er >= n || ec >= n) return false
        if (h) {
            if (!empty(g, r, c - 1) || !empty(g, r, ec + 1)) return false
        } else {
            if (!empty(g, r - 1, c) || !empty(g, er + 1, c)) return false
        }
        val bit = if (h) 1 else 2
        var cross = 0
        for (i in a.indices) {
            val rr = if (h) r else r + i
            val cc = if (h) c + i else c
            if (g[rr][cc] != EMPTY) {
                if (g[rr][cc] != a[i]) return false
                if ((dir[rr][cc] and bit) != 0) return false
                cross++
            } else {
                if (h) {
                    if (!empty(g, rr - 1, cc) || !empty(g, rr + 1, cc)) return false
                } else {
                    if (!empty(g, rr, cc - 1) || !empty(g, rr, cc + 1)) return false
                }
            }
        }
        return cross >= 1 && cross < a.length
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
        for (attempt in 0 until 80) {
            val g = Array(size) { CharArray(size) { EMPTY } }
            val dir = Array(size) { IntArray(size) }
            val cand = cand0.shuffled(rnd)
            val first = cand.take(6).maxByOrNull { it.answer.length } ?: return null
            val r0 = (size / 2 + rnd.nextInt(-1, 2)).coerceIn(0, size - 1)
            val c0 = rnd.nextInt(0, size - first.answer.length + 1)
            val placed = ArrayList<Placed>()
            val p0 = Placed(first, r0, c0, true)
            put(g, dir, p0); placed.add(p0)
            var progress = true
            while (placed.size < target && progress) {
                progress = false
                for (w in cand) {
                    if (placed.size >= target) break
                    if (placed.any { it.entry.answer == w.answer }) continue
                    val opts = ArrayList<Placed>()
                    for (p in placed) {
                        for (i in w.answer.indices) for (j in 0 until p.len) {
                            if (w.answer[i] != p.entry.answer[j]) continue
                            val h = !p.horizontal
                            val (pr, pc) = p.cell(j)
                            val r = if (h) pr else pr - i
                            val c = if (h) pc - i else pc
                            if (canPlace(g, dir, w.answer, r, c, h)) opts.add(Placed(w, r, c, h))
                        }
                    }
                    if (opts.isNotEmpty()) {
                        val o = opts[rnd.nextInt(opts.size)]
                        put(g, dir, o); placed.add(o); progress = true
                    }
                }
            }
            if (placed.size > best.size) best = placed.toList()
            if (best.size >= target) break
        }
        if (best.size < minWords) return null
        // برش حاشیه‌های خالی
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
