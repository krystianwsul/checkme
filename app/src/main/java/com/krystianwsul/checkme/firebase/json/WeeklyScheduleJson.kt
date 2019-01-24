package com.krystianwsul.checkme.firebase.json

class WeeklyScheduleJson @JvmOverloads constructor(
        startTime: Long = 0,
        endTime: Long? = null,
        val dayOfWeek: Int = 0,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson(startTime, endTime)