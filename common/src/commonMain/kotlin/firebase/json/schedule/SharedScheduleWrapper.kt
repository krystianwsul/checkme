package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: SharedSingleScheduleJson? = null,
        val weeklyScheduleJson: SharedWeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: SharedMonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: SharedMonthlyWeekScheduleJson? = null,
        val yearlyScheduleJson: SharedYearlyScheduleJson? = null,
) : ScheduleWrapper