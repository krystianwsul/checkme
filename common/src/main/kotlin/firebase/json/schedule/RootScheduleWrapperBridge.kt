package com.krystianwsul.common.firebase.json.schedule

class RootScheduleWrapperBridge(rootScheduleWrapper: RootScheduleWrapper)
    : ScheduleWrapperBridge {

    override val singleScheduleJson = rootScheduleWrapper.singleScheduleJson
    override val weeklyScheduleJson = rootScheduleWrapper.weeklyScheduleJson
    override val monthlyDayScheduleJson = rootScheduleWrapper.monthlyDayScheduleJson
    override val monthlyWeekScheduleJson = rootScheduleWrapper.monthlyWeekScheduleJson
    override val yearlyScheduleJson = rootScheduleWrapper.yearlyScheduleJson
}