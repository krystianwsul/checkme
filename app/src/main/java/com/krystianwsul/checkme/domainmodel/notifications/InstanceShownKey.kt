package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.common.time.TimeDescriptor

data class InstanceShownKey(
    val id: Int,
    val taskId: String,
    val scheduleYear: Int,
    val scheduleMonth: Int,
    val scheduleDay: Int,
    val scheduleTimeDescriptor: TimeDescriptor,
    val notified: Boolean,
    val notificationShown: Boolean,
    val projectId: String,
)