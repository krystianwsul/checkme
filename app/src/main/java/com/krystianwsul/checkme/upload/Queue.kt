package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import com.jakewharton.rxrelay2.BehaviorRelay
import com.pacoworks.rxpaper2.RxPaperBook

object Queue {

    private const val KEY = "imageUploadQueue"

    private val book = RxPaperBook.with()

    private lateinit var entries: MutableList<Entry>

    val ready = BehaviorRelay.create<Unit>()

    @SuppressLint("CheckResult")
    fun init() {
        check(!ready.hasValue())

        book.read(KEY, mutableListOf<Entry>()).subscribe { entries ->
            check(!ready.hasValue())

            this.entries = entries

            ready.accept(Unit)
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