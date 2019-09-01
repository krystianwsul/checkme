package com.krystianwsul.common.firebase

class DailyScheduleJson @JvmOverloads constructor(
        startTime: Long = 0,
        endTime: Long? = null,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson(startTime, endTime)
