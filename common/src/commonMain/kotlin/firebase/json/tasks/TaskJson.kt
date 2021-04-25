package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import kotlinx.serialization.Serializable

interface TaskJson {

    var name: String
    var note: String?
    var image: Image?
    var ordinal: Double?

    val startTime: Long
    val startTimeOffset: Double? // this is nullable only for project tasks

    var endTime: Long?
    var endData: EndData?

    var instances: MutableMap<String, InstanceJson>
    val schedules: Map<String, ScheduleWrapper>
    var noScheduleOrParent: Map<String, NoScheduleOrParentJson>
    val taskHierarchies: Map<String, NestedTaskHierarchyJson>

    @Serializable
    data class Image(
            val imageUuid: String = "",
            val uploaderUuid: String? = null,
    )

    @Serializable
    data class EndData(
            val time: Long = 0,
            val offset: Double? = null,
            val deleteInstances: Boolean = false,
    )
}