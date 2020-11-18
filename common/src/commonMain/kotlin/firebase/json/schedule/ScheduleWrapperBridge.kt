package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface ScheduleWrapperBridge<T : ProjectType> {

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun <T : ProjectType> fromScheduleWrapper(scheduleWrapper: ScheduleWrapper<T>): ScheduleWrapperBridge<T> = when (scheduleWrapper) {
            is PrivateScheduleWrapper -> PrivateScheduleWrapperBridge(scheduleWrapper)
            is SharedScheduleWrapper -> SharedScheduleWrapperBridge(scheduleWrapper)
            else -> throw IllegalArgumentException()
        } as ScheduleWrapperBridge<T>
    }

    val singleScheduleJson: SingleScheduleJson<T>?
    val weeklyScheduleJson: WeeklyScheduleJson<T>?
    val monthlyDayScheduleJson: MonthlyDayScheduleJson<T>?
    val monthlyWeekScheduleJson: MonthlyWeekScheduleJson<T>?
    val yearlyScheduleJson: YearlyScheduleJson<T>?
}