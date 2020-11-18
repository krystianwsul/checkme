package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedScheduleWrapper @JvmOverloads constructor(
        override val singleScheduleJson: SharedSingleScheduleJson? = null,
        override val weeklyScheduleJson: SharedWeeklyScheduleJson? = null,
        override val monthlyDayScheduleJson: SharedMonthlyDayScheduleJson? = null,
        override val monthlyWeekScheduleJson: SharedMonthlyWeekScheduleJson? = null,
        override val yearlyScheduleJson: SharedYearlyScheduleJson? = null,
) : ScheduleWrapper