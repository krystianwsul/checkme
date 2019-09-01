package com.krystianwsul.common.firebase


abstract class ScheduleJson(val startTime: Long = 0, var endTime: Long? = null) {

    init {
        check(endTime == null || startTime <= endTime!!)
    }
}
