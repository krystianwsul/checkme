package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class NoScheduleOrParentJson @JvmOverloads constructor(
        val startTime: Long = 0,
        var startTimeOffset: Double? = null, // this is nullable only for project tasks
        var endTime: Long? = null,
        var endTimeOffset: Double? = null,
        val projectId: String? = null,
)