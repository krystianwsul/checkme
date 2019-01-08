package com.krystianwsul.checkme.firebase.json

import com.krystianwsul.checkme.utils.time.Date

class OldestVisibleJson(
        var year: Int = 0,
        var month: Int = 0,
        var day: Int = 0) {

    constructor(date: Date) : this(date.year, date.month, date.day)

    fun toDate() = Date(year, month, day)
}