package com.krystianwsul.common.firebase.json.schedule

class SharedScheduleWrapperBridge(sharedScheduleWrapper: SharedScheduleWrapper)
    : ScheduleWrapperBridge {

    override val singleScheduleJson = sharedScheduleWrapper.singleScheduleJson
    override val weeklyScheduleJson = sharedScheduleWrapper.weeklyScheduleJson
    override val monthlyDayScheduleJson = sharedScheduleWrapper.monthlyDayScheduleJson
    override val monthlyWeekScheduleJson = sharedScheduleWrapper.monthlyWeekScheduleJson
    override val yearlyScheduleJson = sharedScheduleWrapper.yearlyScheduleJson
}