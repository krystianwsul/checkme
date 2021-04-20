package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class RootScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: RootSingleScheduleJson? = null,
        val weeklyScheduleJson: RootWeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: RootMonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: RootMonthlyWeekScheduleJson? = null,
        val yearlyScheduleJson: RootYearlyScheduleJson? = null,
) : ScheduleWrapper