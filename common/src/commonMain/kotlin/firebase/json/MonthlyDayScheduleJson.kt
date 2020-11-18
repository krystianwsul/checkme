package com.krystianwsul.common.firebase.json

interface MonthlyDayScheduleJson : RepeatingScheduleJson {

    val dayOfMonth: Int
    val beginningOfMonth: Boolean
}
