package com.krystianwsul.common.firebase.json.schedule

interface SingleScheduleJson : ScheduleJson {

    val year: Int
    val month: Int
    val day: Int
}