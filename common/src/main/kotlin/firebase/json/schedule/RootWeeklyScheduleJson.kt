package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable

@Serializable
data class RootWeeklyScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var startTimeOffset: Double = 0.0,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override val dayOfWeek: Int = 0,
        override val from: String? = null,
        override val until: String? = null,
        override val interval: Int = 1,
        override var oldestVisible: String? = null,
        override var oldestVisibleJson: String? = null,
        override val assignedTo: Map<String, Boolean> = mapOf(),
        override val time: String = "",
        override var projectId: String = "",
        override var projectKey: String? = null, // todo projectKey check constructors, setters, and getters
) : RootScheduleJson, WeeklyScheduleJson, AssignedToJson