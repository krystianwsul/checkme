package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class ScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: SingleScheduleJson? = null,
        val dailyScheduleJson: DailyScheduleJson? = null,
        val weeklyScheduleJson: WeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null)