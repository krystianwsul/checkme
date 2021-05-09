package com.krystianwsul.common.firebase.json.noscheduleorparent

interface NoScheduleOrParentJson {

        val startTime: Long
        val startTimeOffset: Double?

        var endTime: Long?
        var endTimeOffset: Double?
}