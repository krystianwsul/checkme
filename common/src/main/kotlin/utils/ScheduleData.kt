package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.TimePair

sealed interface ScheduleData : Parcelable {

    val timePair: TimePair

    sealed interface Reusable : ScheduleData

    @Parcelize
    data class Single(val date: Date, override val timePair: TimePair) : Reusable

    @Parcelize
    data class Weekly(
        val daysOfWeek: Set<DayOfWeek>,
        override val timePair: TimePair,
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
        override val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class MonthlyWeek(
        val weekOfMonth: Int,
        val dayOfWeek: DayOfWeek,
        val beginningOfMonth: Boolean,
        override val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class Yearly(
        val month: Int,
        val day: Int,
        override val timePair: TimePair,
        val from: Date?,
        val until: Date?,
    ) : ScheduleData

    @Parcelize
    data class Child(val parentInstanceKey: InstanceKey) : Reusable {

        override val timePair: TimePair
            get() = TODO("todo join child")
    }
}