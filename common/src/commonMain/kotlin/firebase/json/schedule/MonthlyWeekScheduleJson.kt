package com.krystianwsul.common.firebase.json.schedule

interface MonthlyWeekScheduleJson : RepeatingScheduleJson {

    val dayOfMonth: Int
    val dayOfWeek: Int
    val beginningOfMonth: Boolean
}