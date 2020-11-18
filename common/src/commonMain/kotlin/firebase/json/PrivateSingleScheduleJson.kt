package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateSingleScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override val year: Int = 0,
        override val month: Int = 0,
        override val day: Int = 0,
        override val customTimeId: String? = null,
        override val hour: Int? = null,
        override val minute: Int? = null,
        override val group: Boolean = false,
) : SingleScheduleJson