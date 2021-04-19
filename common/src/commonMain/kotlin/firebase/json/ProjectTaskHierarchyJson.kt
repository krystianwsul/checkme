package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class ProjectTaskHierarchyJson @JvmOverloads constructor(
        override val parentTaskId: String = "",
        val childTaskId: String = "",
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
) : TaskHierarchyJson