package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import kotlinx.serialization.Serializable

interface TaskJson {

    var name: String
    var note: String?
    var image: Image?
    var ordinal: Double?

    val startTime: Long
    val startTimeOffset: Double?

    var endData: EndData?

    var instances: MutableMap<String, InstanceJson>
    val schedules: Map<String, ScheduleWrapper>
    val noScheduleOrParent: Map<String, NoScheduleOrParentJson>
    val taskHierarchies: Map<String, NestedTaskHierarchyJson>

    @Serializable
    data class Image(
        val imageUuid: String = "",
        val uploaderUuid: String? = null,
    )

    @Serializable
    data class EndData(
        val time: Long = 0,
        val offset: Double? = null, // todo task edit
        val deleteInstances: Boolean = false,
    )
}