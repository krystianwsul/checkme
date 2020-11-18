package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType

interface YearlyScheduleJson<T : ProjectType> : RepeatingScheduleJson<T> {

    val month: Int
    val day: Int
}