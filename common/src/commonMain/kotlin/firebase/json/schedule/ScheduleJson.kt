package com.krystianwsul.common.firebase.json.schedule


interface ScheduleJson {

    val startTime: Long
    var startTimeOffset: Double? // this is nullable only for project tasks

    var endTime: Long?
    var endTimeOffset: Double?

    val customTimeId: String?
    val hour: Int?
    val minute: Int?

    val time: String?
}
