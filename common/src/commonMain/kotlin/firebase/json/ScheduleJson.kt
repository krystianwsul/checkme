package com.krystianwsul.common.firebase.json


interface ScheduleJson {

    val startTime: Long
    var endTime: Long?

    val customTimeId: String?
    val hour: Int?
    val minute: Int?
}
