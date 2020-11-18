package com.krystianwsul.common.firebase.json

interface MonthlyWeekScheduleJson : RepeatingScheduleJson {

    val dayOfMonth: Int
    val dayOfWeek: Int
    val beginningOfMonth: Boolean
}