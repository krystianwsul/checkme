package com.krystianwsul.common.firebase.json.schedule

interface RepeatingScheduleJson : ScheduleJson {

    val from: String?
    val until: String?

    var oldestVisible: String?
    var oldestVisibleJson: String?
}