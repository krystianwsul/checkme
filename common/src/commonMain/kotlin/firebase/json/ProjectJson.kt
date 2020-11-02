package com.krystianwsul.common.firebase.json

interface ProjectJson {

    var name: String

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endTimeOffset: Double?

    var tasks: MutableMap<String, TaskJson>
    var taskHierarchies: MutableMap<String, TaskHierarchyJson>
    val customTimes: Map<String, CustomTimeJson>
}