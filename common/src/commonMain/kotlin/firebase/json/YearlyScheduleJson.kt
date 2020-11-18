package com.krystianwsul.common.firebase.json

interface YearlyScheduleJson : RepeatingScheduleJson {

    val month: Int
    val day: Int
}