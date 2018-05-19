package com.krystianwsul.checkme.firebase.json

class MonthlyDayScheduleJson(
        startTime: Long = 0,
        endTime: Long? = null,
        val dayOfMonth: Int = 0,
        val beginningOfMonth: Boolean = false,
        val customTimeId: String?,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson(startTime, endTime)
