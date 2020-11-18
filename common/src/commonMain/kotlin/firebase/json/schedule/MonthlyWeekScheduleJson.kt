package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface MonthlyWeekScheduleJson<T : ProjectType> : RepeatingScheduleJson<T> {

    val dayOfMonth: Int
    val dayOfWeek: Int
    val beginningOfMonth: Boolean
}