package com.krystianwsul.common.firebase.json.schedule

interface MonthlyDayScheduleJson : RepeatingScheduleJson {

    val dayOfMonth: Int
    val beginningOfMonth: Boolean
}
