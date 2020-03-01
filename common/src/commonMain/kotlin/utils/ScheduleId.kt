package com.krystianwsul.common.utils

sealed class ScheduleId {

    data class Remote(val projectId: ProjectKey, val taskId: String, val scheduleId: String) : ScheduleId()
}