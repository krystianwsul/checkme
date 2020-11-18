package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateMonthlyDayScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override val dayOfMonth: Int = 0,
        override val beginningOfMonth: Boolean = false,
        override val customTimeId: String? = null,
        override val hour: Int? = null,
        override val minute: Int? = null,
        override val from: String? = null,
        override val until: String? = null,
        override var oldestVisible: String? = null,
) : MonthlyDayScheduleJson
