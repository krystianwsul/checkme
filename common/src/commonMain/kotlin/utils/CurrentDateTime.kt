package com.krystianwsul.common.utils

import com.krystianwsul.common.time.DateTime

interface CurrentDateTime {

    val startDateTime: DateTime
    val endDateTime: DateTime?

    fun notDeletedDateTime(dateTime: DateTime) = endDateTime?.let { it > dateTime } != false

    fun afterStartDateTime(dateTime: DateTime) = startDateTime <= dateTime

    fun currentDateTime(dateTime: DateTime) = afterStartDateTime(dateTime) && notDeletedDateTime(dateTime)

    fun requireNotDeletedDateTime(dateTime: DateTime) {
        if (!notDeletedDateTime(dateTime))
            throwTime(dateTime)
    }

    fun requireCurrentDateTime(dateTime: DateTime) {
        if (!currentDateTime(dateTime))
            throwTime(dateTime)
    }

    fun requireNotCurrentDateTime(dateTime: DateTime) {
        if (currentDateTime(dateTime))
            throwTime(dateTime)
    }

    private fun throwTime(dateTime: DateTime): Nothing = throw Current.TimeException(
            "$this dateTimes start: $startDateTime, end: $endDateTime, time: $dateTime"
    )
}