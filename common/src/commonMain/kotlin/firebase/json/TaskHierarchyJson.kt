package com.krystianwsul.common.firebase.json

import kotlin.jvm.JvmOverloads

@Serializable
data class TaskHierarchyJson @JvmOverloads constructor(
        val parentTaskId: String = "",
        val childTaskId: String = "",
        val startTime: Long = 0,
        var startTimeOffset: Double? = null,
        var endTime: Long? = null,
        var endTimeOffset: Double? = null,
)