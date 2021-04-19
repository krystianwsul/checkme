package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class NestedTaskHierarchyJson @JvmOverloads constructor(
        override val parentTaskId: String = "",
        override val startTime: Long = 0,
        override val startTimeOffset: Double = 0.0,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
) : TaskHierarchyJson