package com.krystianwsul.common.firebase.json.schedule


interface ScheduleJson {

    val startTime: Long
    val startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?

    val time: String?
}
