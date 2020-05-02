package com.krystianwsul.common.firebase.json

interface RepeatingScheduleJson : ScheduleJson {

    val from: String?
    val until: String?
}