package com.krystianwsul.common.firebase.json

interface ScheduleWrapper {

    val singleScheduleJson: SingleScheduleJson?
    val weeklyScheduleJson: WeeklyScheduleJson?
    val monthlyDayScheduleJson: MonthlyDayScheduleJson?
    val monthlyWeekScheduleJson: MonthlyWeekScheduleJson?
    val yearlyScheduleJson: YearlyScheduleJson?
}