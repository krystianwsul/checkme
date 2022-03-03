package com.krystianwsul.common.firebase.json.schedule

interface RepeatingScheduleJson : ScheduleJson {

    val from: String?
    val until: String?

    var oldestVisible: String? // todo remove once all phones upgrade past > 0.75.1
    var oldestVisibleJson: String?
}