package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.customtimes.CustomTimeJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson

interface OwnedProjectJson : ProjectJson {

    val startTime: Long
    var startTimeOffset: Double?

    val tasks: Map<String, TaskJson>
    var taskHierarchies: MutableMap<String, ProjectTaskHierarchyJson>
    val customTimes: Map<String, CustomTimeJson>
}