package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.time.Date
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class OldestVisibleJson @JvmOverloads constructor(
        var date: String? = null,
        var year: Int = 0,
        var month: Int = 0,
        var day: Int = 0
) {

    companion object {

        fun fromDate(date: Date) = OldestVisibleJson(date.toJson(), date.year, date.month, date.day)
    }

    fun toDate() = date?.let { Date.fromJson(it) } ?: Date(year, month, day)
}