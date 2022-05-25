package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.RootTaskParentJson
import com.krystianwsul.common.firebase.json.customtimes.CustomTimeJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.utils.ProjectType

interface ProjectJson<T : ProjectType> : RootTaskParentJson {

    val startTime: Long
    var startTimeOffset: Double?

    val tasks: Map<String, TaskJson>
    var taskHierarchies: MutableMap<String, ProjectTaskHierarchyJson>
    val customTimes: Map<String, CustomTimeJson>
}