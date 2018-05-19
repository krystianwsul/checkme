package com.krystianwsul.checkme.firebase.json

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class ScheduleWrapper(
        val singleScheduleJson: SingleScheduleJson? = null,
        val dailyScheduleJson: DailyScheduleJson? = null,
        val weeklyScheduleJson: WeeklyScheduleJson? = null,
        val monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
        val monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null)