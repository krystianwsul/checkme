package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.utils.ProjectType

interface ProjectJson<T : ProjectType> {

    var name: String

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?

    val tasks: Map<String, TaskJson<T>>
    var taskHierarchies: MutableMap<String, ProjectTaskHierarchyJson>
    val customTimes: Map<String, CustomTimeJson>
}