package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import android.net.Uri
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.utils.TaskKey
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

    fun write() {
        book.write(KEY, entries).subscribe()
    }

    fun addEntry(taskKey: TaskKey, uuid: String, path: String, uri: Uri): Entry {
        val entry = Entry(taskKey, uuid, path, uri, null)
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
            val taskKey: TaskKey,
            val uuid: String,
            val path: String,
            val fileUri: Uri,
            var sessionUri: Uri?)
}