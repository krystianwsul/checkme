package com.krystianwsul.common.firebase.json

interface WeeklyScheduleJson : RepeatingScheduleJson {

    val dayOfWeek: Int
    val interval: Int
}