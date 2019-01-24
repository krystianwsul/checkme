package com.krystianwsul.checkme.firebase.json

class MonthlyWeekScheduleJson @JvmOverloads constructor(
        startTime: Long = 0,
        endTime: Long? = null,
        val dayOfMonth: Int = 0,
        val dayOfWeek: Int = 0,
        val beginningOfMonth: Boolean = false,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson(startTime, endTime)