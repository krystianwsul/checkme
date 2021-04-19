package com.krystianwsul.common.firebase.json.schedule

class PrivateScheduleWrapperBridge(privateScheduleWrapper: PrivateScheduleWrapper)
    : ScheduleWrapperBridge {

    override val singleScheduleJson = privateScheduleWrapper.singleScheduleJson
    override val weeklyScheduleJson = privateScheduleWrapper.weeklyScheduleJson
    override val monthlyDayScheduleJson = privateScheduleWrapper.monthlyDayScheduleJson
    override val monthlyWeekScheduleJson = privateScheduleWrapper.monthlyWeekScheduleJson
    override val yearlyScheduleJson = privateScheduleWrapper.yearlyScheduleJson
}