package com.krystianwsul.checkme.utils

sealed class ScheduleId {

    data class Local(val id: Int) : ScheduleId()
    data class Remote(val projectId: String, val taskId: String, val scheduleId: String) : ScheduleId()
}