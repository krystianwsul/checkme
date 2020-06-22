package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.TimePair

sealed class ScheduleData : Serializable {

    abstract val timePair: TimePair

    data class Single(
            val date: Date,
            override val timePair: TimePair
    ) : ScheduleData()

    data class Weekly(
            val daysOfWeek: Set<DayOfWeek>,
            override val timePair: TimePair,
            val from: Date?,
            val until: Date?,
            val interval: Int
    ) : ScheduleData()

    data class MonthlyDay(
            val dayOfMonth: Int,
            val beginningOfMonth: Boolean,
            override val timePair: TimePair,
            val from: Date?,
            val until: Date?
    ) : ScheduleData()

    data class MonthlyWeek(
            val dayOfMonth: Int,
            val dayOfWeek: DayOfWeek,
            val beginningOfMonth: Boolean,
            override val timePair: TimePair,
            val from: Date?,
            val until: Date?
    ) : ScheduleData()

    data class Yearly(
            val month: Int,
            val day: Int,
            override val timePair: TimePair,
            val from: Date?,
            val until: Date?
    ) : ScheduleData()
}