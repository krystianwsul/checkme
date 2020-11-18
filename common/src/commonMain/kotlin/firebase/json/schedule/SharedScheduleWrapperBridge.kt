package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

class SharedScheduleWrapperBridge(sharedScheduleWrapper: SharedScheduleWrapper)
    : ScheduleWrapperBridge<ProjectType.Shared> {

    override val singleScheduleJson = sharedScheduleWrapper.singleScheduleJson
    override val weeklyScheduleJson = sharedScheduleWrapper.weeklyScheduleJson
    override val monthlyDayScheduleJson = sharedScheduleWrapper.monthlyDayScheduleJson
    override val monthlyWeekScheduleJson = sharedScheduleWrapper.monthlyWeekScheduleJson
    override val yearlyScheduleJson = sharedScheduleWrapper.yearlyScheduleJson
}