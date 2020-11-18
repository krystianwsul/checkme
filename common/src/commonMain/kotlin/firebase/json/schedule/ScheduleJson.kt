package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType


interface ScheduleJson<T : ProjectType> {

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?

    val customTimeId: String?
    val hour: Int?
    val minute: Int?
}
