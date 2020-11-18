package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface WeeklyScheduleJson<T : ProjectType> : RepeatingScheduleJson<T> {

    val dayOfWeek: Int
    val interval: Int
}