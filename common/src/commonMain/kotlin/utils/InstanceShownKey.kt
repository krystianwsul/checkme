package com.krystianwsul.common.utils

import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimeDescriptor

data class InstanceShownKey(
    val taskId: String,
    val scheduleYear: Int,
    val scheduleMonth: Int,
    val scheduleDay: Int,
    val scheduleTimeDescriptor: TimeDescriptor,
    val projectId: String,
) {

    companion object {

        private fun DateTime.getDescriptor() = TimeDescriptor.fromJsonTime(JsonTime.fromTime(time))
    }

    constructor(taskKeyData: TaskKeyData, scheduleDateTime: DateTime) : this(
        taskKeyData.taskId,
        scheduleDateTime.date.year,
        scheduleDateTime.date.month,
        scheduleDateTime.date.day,
        scheduleDateTime.getDescriptor(),
        taskKeyData.projectId,
    )

    val taskKeyData get() = TaskKeyData(projectId, taskId) // todo local
}