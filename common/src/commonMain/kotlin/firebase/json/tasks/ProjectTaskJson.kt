package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.ProjectNoScheduleOrParentJson

interface ProjectTaskJson : TaskJson {

    var endTime: Long?

    override val noScheduleOrParent: Map<String, ProjectNoScheduleOrParentJson>
}