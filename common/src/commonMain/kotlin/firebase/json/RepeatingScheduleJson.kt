package com.krystianwsul.common.firebase.json

interface RepeatingScheduleJson : ScheduleJson {

    val from: String?
    val until: String?

    var oldestVisible: String? // todo schedule oldestVisible take into account when copying task
}