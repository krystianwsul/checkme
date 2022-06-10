package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.TimePair

sealed interface ScheduleData : Parcelable {

    sealed interface Reusable : ScheduleData

    @Parcelize
    data class Single(val dateTimePair: DateTimePair) : Reusable {

        constructor(date: Date, timePair: TimePair) : this(DateTimePair(date, timePair))

        val date get() = dateTimePair.date
        val timePair get() = dateTimePair.timePair
    }

    @Parcelize
    data class Weekly(
        val daysOfWeek: Set<DayOfWeek>,
        val timePair: TimePair,
        val from: Date?,
        val until: Date?,
        val interval: Int,
    ) : ScheduleData {

        init {
            check(interval > 0)
        }
    }

    @Parcelize
    data class MonthlyDay(
        val dayOfMonth: Int,
        val beginningOfMonth: Boolean,
        val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class MonthlyWeek(
        val weekOfMonth: Int,
        val dayOfWeek: DayOfWeek,
        val beginningOfMonth: Boolean,
        val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class Yearly(
        val month: Int,
        val day: Int,
        val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class Child(val parentInstanceKey: InstanceKey) : Reusable
}