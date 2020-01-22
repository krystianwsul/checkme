package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SingleScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        val year: Int = 0,
        val month: Int = 0,
        val day: Int = 0,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null
) : ScheduleJson