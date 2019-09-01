package com.krystianwsul.common.firebase

class ScheduleWrapper @JvmOverloads constructor(
        val singleScheduleJson: SingleScheduleJson? = null,
        val dailyScheduleJson: DailyScheduleJson? = null,
        val weeklyScheduleJson: WeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null)