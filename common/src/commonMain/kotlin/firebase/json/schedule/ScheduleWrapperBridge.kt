package com.krystianwsul.common.firebase.json.schedule

interface ScheduleWrapperBridge {

    companion object {

        fun fromScheduleWrapper(scheduleWrapper: ScheduleWrapper): ScheduleWrapperBridge = when (scheduleWrapper) {
            is PrivateScheduleWrapper -> PrivateScheduleWrapperBridge(scheduleWrapper)
            is SharedScheduleWrapper -> SharedScheduleWrapperBridge(scheduleWrapper)
            else -> throw IllegalArgumentException()
        }
    }

    val singleScheduleJson: SingleScheduleJson?
    val weeklyScheduleJson: WeeklyScheduleJson?
    val monthlyDayScheduleJson: MonthlyDayScheduleJson?
    val monthlyWeekScheduleJson: MonthlyWeekScheduleJson?
    val yearlyScheduleJson: YearlyScheduleJson?
}