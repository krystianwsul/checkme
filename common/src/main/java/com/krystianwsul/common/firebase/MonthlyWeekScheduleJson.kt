package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable

@Serializable
class MonthlyWeekScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        val dayOfMonth: Int = 0,
        val dayOfWeek: Int = 0,
        val beginningOfMonth: Boolean = false,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson