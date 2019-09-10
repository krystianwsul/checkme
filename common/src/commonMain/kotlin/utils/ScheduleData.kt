package com.krystianwsul.common.utils

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.TimePair

sealed class ScheduleData : Serializable {

    abstract val timePair: TimePair

    data class Single(
            val date: Date,
            override val timePair: TimePair) : ScheduleData()

    data class Weekly(
            val daysOfWeek: Set<DayOfWeek>,
            override val timePair: TimePair) : ScheduleData()

    data class MonthlyDay(
            val dayOfMonth: Int,
            val beginningOfMonth: Boolean,
            override val timePair: TimePair) : ScheduleData()

    data class MonthlyWeek(
            val dayOfMonth: Int,
            val dayOfWeek: DayOfWeek,
            val beginningOfMonth: Boolean,
            override val timePair: TimePair) : ScheduleData()
}