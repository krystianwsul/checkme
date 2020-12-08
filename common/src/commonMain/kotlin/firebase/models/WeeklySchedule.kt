package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.schedule.WeeklyScheduleRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType

class WeeklySchedule<T : ProjectType>(
        rootTask: Task<T>,
        override val repeatingScheduleRecord: WeeklyScheduleRecord<T>,
) : RepeatingSchedule<T>(rootTask) {

    val dayOfWeek = DayOfWeek.values()[repeatingScheduleRecord.dayOfWeek]

    override val scheduleRecord get() = repeatingScheduleRecord

    override val scheduleType = ScheduleType.WEEKLY

    val interval = repeatingScheduleRecord.interval

    override fun containsDate(date: Date): Boolean {
        val day = date.dayOfWeek

        if (dayOfWeek != day) return false

        if (interval != 1) {
            val timeSpan = date.toDateSoy().dateTimeDayStart - from!!.toDateSoy().dateTimeDayStart
            if (timeSpan.weeks.toInt().rem(interval) != 0) return false
        }

        return true
    }
}
