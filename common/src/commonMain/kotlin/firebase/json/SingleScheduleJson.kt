package com.krystianwsul.common.firebase.json

interface SingleScheduleJson : ScheduleJson {

    val year: Int
    val month: Int
    val day: Int

    val group: Boolean
}