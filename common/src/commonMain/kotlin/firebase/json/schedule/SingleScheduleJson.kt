package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface SingleScheduleJson<T : ProjectType> : ScheduleJson<T> {

    val year: Int
    val month: Int
    val day: Int
}