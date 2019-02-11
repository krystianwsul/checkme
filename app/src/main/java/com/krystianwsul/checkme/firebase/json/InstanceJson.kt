package com.krystianwsul.checkme.firebase.json

import android.util.Log

class InstanceJson @JvmOverloads constructor(
        var done: Long? = null,
        var instanceDate: String? = null,
        var instanceYear: Int? = null,
        var instanceMonth: Int? = null,
        var instanceDay: Int? = null,
        var instanceTime: String? = null,
        var instanceCustomTimeId: String? = null,
        var instanceHour: Int? = null,
        var instanceMinute: Int? = null,
        var ordinal: Double? = null) {


    init {
        if (instanceCustomTimeId == "12:00")
            Log.e("asdf", "magic")
    }
}