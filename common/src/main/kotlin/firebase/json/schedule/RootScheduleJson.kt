package com.krystianwsul.common.firebase.json.schedule

interface RootScheduleJson : ScheduleJson {

    override val startTimeOffset: Double

    override val time: String

    var projectId: String
}