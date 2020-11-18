package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import kotlinx.serialization.Serializable

interface TaskJson {

    var name: String
    var note: String?
    var image: Image?
    var ordinal: Double?

    val startTime: Long
    var startTimeOffset: Double?

    var endTime: Long?
    var endData: EndData?

    var instances: MutableMap<String, InstanceJson>
    val schedules: Map<String, ScheduleWrapper>
    var noScheduleOrParent: Map<String, NoScheduleOrParentJson>

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