package com.krystianwsul.common.firebase.json.schedule

interface YearlyScheduleJson : RepeatingScheduleJson {

    val month: Int
    val day: Int
}