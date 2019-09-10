package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class DailyScheduleJson @JvmOverloads constructor(
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        val customTimeId: String? = null,
        val hour: Int? = null,
        val minute: Int? = null) : ScheduleJson
