package com.krystianwsul.common.firebase.json.noscheduleorparent

interface NoScheduleOrParentJson {

        val startTime: Long
        var startTimeOffset: Double? // this is nullable only for project tasks

        var endTime: Long?
        var endTimeOffset: Double?

        var projectId: String? // this is nullable only for project tasks
}