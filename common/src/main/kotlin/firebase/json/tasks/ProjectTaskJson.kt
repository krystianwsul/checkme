package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson
import kotlinx.serialization.Serializable

interface ProjectTaskJson : TaskJson {

    var endTime: Long?
    var endData: EndData?

    override val noScheduleOrParent: Map<String, ProjectNoScheduleOrParentJson>

    @Serializable
    data class EndData(
        override val time: Long = 0,
        override val offset: Double? = null,
        override val deleteInstances: Boolean = false,
    ) : TaskJson.EndData
}