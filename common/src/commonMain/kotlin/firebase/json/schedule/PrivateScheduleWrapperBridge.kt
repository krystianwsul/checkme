package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

class PrivateScheduleWrapperBridge(privateScheduleWrapper: PrivateScheduleWrapper)
    : ScheduleWrapperBridge<ProjectType.Private> {

    override val singleScheduleJson = privateScheduleWrapper.singleScheduleJson
    override val weeklyScheduleJson = privateScheduleWrapper.weeklyScheduleJson
    override val monthlyDayScheduleJson = privateScheduleWrapper.monthlyDayScheduleJson
    override val monthlyWeekScheduleJson = privateScheduleWrapper.monthlyWeekScheduleJson
    override val yearlyScheduleJson = privateScheduleWrapper.yearlyScheduleJson
}