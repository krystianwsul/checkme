package com.krystianwsul.common.utils

sealed class ScheduleId {

    data class Remote(val projectId: String, val taskId: String, val scheduleId: String) : ScheduleId()
}