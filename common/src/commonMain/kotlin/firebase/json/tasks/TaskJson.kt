package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import kotlinx.serialization.Serializable

interface TaskJson {

    val name: String
    val note: String?
    var image: Image? // todo task json
    var ordinal: Double?

    val startTime: Long
    val startTimeOffset: Double?

    val endData: EndData?

    var instances: MutableMap<String, InstanceJson>
    val schedules: Map<String, ScheduleWrapper>
    val noScheduleOrParent: Map<String, NoScheduleOrParentJson>
    val taskHierarchies: Map<String, NestedTaskHierarchyJson>

    @Serializable
    data class Image(
        val imageUuid: String = "",
        val uploaderUuid: String? = null,
    )

    interface EndData {

        val time: Long
        val offset: Double?
        val deleteInstances: Boolean
    }
}