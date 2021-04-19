package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: PrivateSingleScheduleJson? = null,
        val weeklyScheduleJson: PrivateWeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: PrivateMonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: PrivateMonthlyWeekScheduleJson? = null,
        val yearlyScheduleJson: PrivateYearlyScheduleJson? = null,
) : ScheduleWrapper