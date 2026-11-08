package com.marzieh.crossword

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.random.Random

/** بانک سؤال‌ها (فایل assets/questions.txt) + دیتابیس SQLite برای ماندگاری شناسه‌ی سؤال‌های استفاده‌شده. */
class Bank(private val ctx: Context) : SQLiteOpenHelper(ctx, "crossword.db", null, 1) {

    val all: List<WordEntry> by lazy { load() }
    val byId: Map<Int, WordEntry> by lazy { all.associateBy { it.id } }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE used(id INTEGER PRIMARY KEY, diff INTEGER NOT NULL, ts INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

    private fun normalize(s: String): String = s.trim()
        .replace('\u064A', '\u06CC').replace('\u0643', '\u06A9')
        .replace("\u200c", "").replace(" ", "")

    private fun load(): List<WordEntry> {
        val out = ArrayList<WordEntry>()
        val seen = HashSet<Int>()
        ctx.assets.open("questions.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (l in lines) {
                val t = l.trim()
                if (t.isEmpty() || t.startsWith("#")) continue
                val p = t.split("|", limit = 5)
                if (p.size < 5) continue
                val id = p[0].trim().toIntOrNull() ?: continue
                val d = p[2].trim().toIntOrNull() ?: continue
                val a = normalize(p[3])
                if (a.length < 2 || !seen.add(id)) continue
                out.add(WordEntry(id, p[1].trim(), d, a, p[4].trim()))
            }
        }
        return out
    }

    fun usedIds(): Set<Int> {
        val s = HashSet<Int>()
        readableDatabase.rawQuery("SELECT id FROM used", null).use { c -> while (c.moveToNext()) s.add(c.getInt(0)) }
        return s
    }

    fun markUsed(ids: List<Int>, diff: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in ids) {
                val v = ContentValues()
                v.put("id", id); v.put("diff", diff); v.put("ts", System.currentTimeMillis())
                db.insertWithOnConflict("used", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun resetDiff(diff: Int) { writableDatabase.delete("used", "diff=?", arrayOf(diff.toString())) }

    fun usedCount(diff: Int): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM used WHERE diff=?", arrayOf(diff.toString())).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun totalCount(diff: Int): Int = all.count { it.diff == diff }

    /** استخر سؤال‌های استفاده‌نشده با توزیع متنوع دسته‌ها. اگر استخر تمام شده باشد چرخه‌ی جدید آغاز می‌شود (reset=true). */
    fun pickPool(diff: Int, rnd: Random): Pair<List<WordEntry>, Boolean> {
        var reset = false
        var avail = all.filter { it.diff == diff && it.id !in usedIds() }
        if (avail.size < 16) {
            resetDiff(diff); reset = true
            avail = all.filter { it.diff == diff }
        }
        val counts = HashMap<String, Int>()
        val pool = ArrayList<WordEntry>()
        for (e in avail.shuffled(rnd)) {
            val n = counts[e.cat] ?: 0
            if (n < 6) { counts[e.cat] = n + 1; pool.add(e) }
            if (pool.size >= 45) break
        }
        return Pair(pool, reset)
    }
}
