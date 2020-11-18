package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface ScheduleWrapper<T : ProjectType> {

    val singleScheduleJson: SingleScheduleJson<T>?
    val weeklyScheduleJson: WeeklyScheduleJson<T>?
    val monthlyDayScheduleJson: MonthlyDayScheduleJson<T>?
    val monthlyWeekScheduleJson: MonthlyWeekScheduleJson<T>?
    val yearlyScheduleJson: YearlyScheduleJson<T>?
}