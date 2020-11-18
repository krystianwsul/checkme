package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface ScheduleWrapper<T : ProjectType> {

    val singleScheduleJson: SingleScheduleJson?
    val weeklyScheduleJson: WeeklyScheduleJson?
    val monthlyDayScheduleJson: MonthlyDayScheduleJson?
    val monthlyWeekScheduleJson: MonthlyWeekScheduleJson?
    val yearlyScheduleJson: YearlyScheduleJson?
}