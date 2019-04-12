package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import com.pacoworks.rxpaper2.RxPaperBook

object Queue {

    private const val KEY = "imageUploadQueue"

    private val book = RxPaperBook.with()

    private lateinit var entries: MutableList<Entry>

    @SuppressLint("CheckResult")
    fun init() {
        book.read(KEY, mutableListOf<Entry>()).subscribe { entries ->
            this.entries = entries
        }
    }

    private fun write() {
        book.write(KEY, entries).subscribe()
    }

    fun addEntry(uuid: String, path: String): Entry {
        val entry = Entry(uuid, path)
        entries.add(entry)
        write()
        return entry
    }

    fun removeEntry(entry: Entry) {
        entries.remove(entry)
        write()
    }

    fun getEntries(): List<Entry> = entries

    fun getPath(uuid: String) = entries.single { it.uuid == uuid }.path

    data class Entry(
            val uuid: String,
            val path: String)
}