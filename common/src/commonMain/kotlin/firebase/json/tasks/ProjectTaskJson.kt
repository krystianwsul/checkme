package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson

interface ProjectTaskJson : TaskJson {

    var endTime: Long?
    override var endData: TaskJson.EndData?

    override val noScheduleOrParent: Map<String, ProjectNoScheduleOrParentJson>
}