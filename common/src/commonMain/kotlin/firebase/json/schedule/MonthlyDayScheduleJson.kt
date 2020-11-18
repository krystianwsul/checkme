package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface MonthlyDayScheduleJson<T : ProjectType> : RepeatingScheduleJson<T> {

    val dayOfMonth: Int
    val beginningOfMonth: Boolean
}
