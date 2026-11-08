package com.marzieh.crossword

const val EMPTY = '\u0000'

data class WordEntry(val id: Int, val cat: String, val diff: Int, val answer: String, val clue: String)

class Placed(val entry: WordEntry, val row: Int, val col: Int, val horizontal: Boolean) {
    var number = 0
    val len: Int get() = entry.answer.length
    fun cell(i: Int): Pair<Int, Int> = if (horizontal) Pair(row, col + i) else Pair(row + i, col)
    fun cells(): List<Pair<Int, Int>> = (0 until len).map { cell(it) }
    fun contains(r: Int, c: Int): Boolean =
        if (horizontal) r == row && c >= col && c < col + len else c == col && r >= row && r < row + len
}

class Puzzle(val rows: Int, val cols: Int, val words: List<Placed>) {
    val solution: Array<CharArray> = Array(rows) { CharArray(cols) { EMPTY } }

    init {
        for (w in words) for (i in 0 until w.len) {
            val (r, c) = w.cell(i)
            solution[r][c] = w.entry.answer[i]
        }
        var n = 0
        // شماره‌گذاری: ردیف به ردیف از بالا، و در هر ردیف از راست به چپ (فارسی)
        for (r in 0 until rows) for (c in cols - 1 downTo 0) {
            val starting = words.filter { it.row == r && it.col == c }
            if (starting.isNotEmpty()) {
                n++
                starting.forEach { it.number = n }
            }
        }
    }

    fun isCell(r: Int, c: Int): Boolean = r in 0 until rows && c in 0 until cols && solution[r][c] != EMPTY
    fun wordAt(r: Int, c: Int, horizontal: Boolean): Placed? =
        words.firstOrNull { it.horizontal == horizontal && it.contains(r, c) }
}
