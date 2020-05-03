package com.krystianwsul.common.utils

// todo figure out naming for this vs. ScheduleKey
data class ScheduleId(val projectId: ProjectKey<*>, val taskId: String, val scheduleId: String)