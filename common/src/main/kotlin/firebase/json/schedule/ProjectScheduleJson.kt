package com.krystianwsul.common.firebase.json.schedule


interface ProjectScheduleJson : ScheduleJson {

    override var startTimeOffset: Double? // this is nullable only for project tasks

    val customTimeId: String?
    val hour: Int?
    val minute: Int?
}
