package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType


interface ScheduleJson<T : ProjectType> {

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?

    val customTimeId: String? // todo customtime use jsonTime below
    val hour: Int?
    val minute: Int?

    val time: String?
}
