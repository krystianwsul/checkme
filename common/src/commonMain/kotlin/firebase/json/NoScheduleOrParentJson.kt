package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class NoScheduleOrParentJson @JvmOverloads constructor(
        val startTime: Long = 0,
        var startTimeOffset: Double? = null,
        var endTime: Long? = null,
        var endTimeOffset: Double? = null
)