package com.krystianwsul.common.firebase.json

interface TaskHierarchyJson {

    val parentTaskId: String

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?
}