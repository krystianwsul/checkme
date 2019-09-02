package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable

@Serializable
class ScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: SingleScheduleJson? = null,
        val dailyScheduleJson: DailyScheduleJson? = null,
        val weeklyScheduleJson: WeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null)