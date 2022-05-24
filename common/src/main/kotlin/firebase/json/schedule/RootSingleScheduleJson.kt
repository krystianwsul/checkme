package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable

@Serializable
data class RootSingleScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var startTimeOffset: Double = 0.0,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override val year: Int = 0,
        override val month: Int = 0,
        override val day: Int = 0,
        override var assignedTo: Map<String, Boolean> = mapOf(),
        override val time: String = "",
        override var projectId: String = "",
        override var projectKey: String? = null,
) : RootScheduleJson, SingleScheduleJson, WriteAssignedToJson