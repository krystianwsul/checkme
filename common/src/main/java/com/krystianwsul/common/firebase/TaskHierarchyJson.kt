package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable

@Serializable
class TaskHierarchyJson @JvmOverloads constructor(
        val parentTaskId: String = "",
        val childTaskId: String = "",
        val startTime: Long = 0,
        var endTime: Long? = null,
        var ordinal: Double? = null)