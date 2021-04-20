package com.krystianwsul.common.firebase.json.taskhierarchies

interface TaskHierarchyJson {

    val parentTaskId: String

    val startTime: Long
    val startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?
}