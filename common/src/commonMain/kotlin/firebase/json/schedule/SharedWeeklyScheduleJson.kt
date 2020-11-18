package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedWeeklyScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override val dayOfWeek: Int = 0,
        override val customTimeId: String? = null,
        override val hour: Int? = null,
        override val minute: Int? = null,
        override val from: String? = null,
        override val until: String? = null,
        override val interval: Int = 1,
        override var oldestVisible: String? = null,
        override val assignedTo: Map<String, Boolean> = mapOf(),
) : WeeklyScheduleJson<ProjectType.Shared>, AssignedToJson