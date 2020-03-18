package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class MonthlyDayScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        val dayOfMonth: Int = 0,
        val beginningOfMonth: Boolean = false,
        override val customTimeId: String? = null,
        override val hour: Int? = null,
        override val minute: Int? = null,
        val from: String? = null,
        val until: String? = null
) : ScheduleJson
