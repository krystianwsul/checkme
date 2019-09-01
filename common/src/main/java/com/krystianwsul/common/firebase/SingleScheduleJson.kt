package com.krystianwsul.common.firebase

class SingleScheduleJson @JvmOverloads constructor(
        startTime: Long = 0,
        endTime: Long? = null,
        val year: Int = 0,
        val month: Int = 0,
        val day: Int = 0,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson(startTime, endTime)