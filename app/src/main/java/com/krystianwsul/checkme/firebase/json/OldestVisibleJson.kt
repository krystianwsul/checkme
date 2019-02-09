package com.krystianwsul.checkme.firebase.json

import com.krystianwsul.checkme.utils.time.Date

class OldestVisibleJson @JvmOverloads constructor(
        var date: String? = null,
        var year: Int = 0,
        var month: Int = 0,
        var day: Int = 0) {

    constructor(date: Date) : this(date.toJson(), date.year, date.month, date.day)

    fun toDate() = date?.let { Date.fromJson(it) } ?: Date(year, month, day)
}