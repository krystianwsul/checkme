package com.krystianwsul.common.firebase.json.schedule

interface WeeklyScheduleJson : RepeatingScheduleJson {

    val dayOfWeek: Int
    val interval: Int
}