package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface RepeatingScheduleJson<T : ProjectType> : ScheduleJson<T> {

    val from: String?
    val until: String?

    var oldestVisible: String?
}