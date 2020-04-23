package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class DailyScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        override val customTimeId: String? = null,
        override val hour: Int? = null,
        override val minute: Int? = null
) : ScheduleJson
