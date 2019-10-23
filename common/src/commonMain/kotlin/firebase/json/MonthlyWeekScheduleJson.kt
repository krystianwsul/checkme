package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class MonthlyWeekScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        val dayOfMonth: Int = 0,
        val dayOfWeek: Int = 0,
        val beginningOfMonth: Boolean = false,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null,
        var from: String? = null,
        var until: String? = null
) : ScheduleJson