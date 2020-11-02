package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class TaskJson @JvmOverloads constructor(
        var name: String = "",
        val startTime: Long = 0,
        var startTimeOffset: Double? = null,
        var endTime: Long? = null,
        var note: String? = null,
        var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        var schedules: MutableMap<String, ScheduleWrapper> = mutableMapOf(),
        var image: Image? = null,
        var endData: EndData? = null,
        var noScheduleOrParent: Map<String, NoScheduleOrParentJson> = mutableMapOf(),
        var ordinal: Double? = null
) {

    @Serializable
    data class Image(
            val imageUuid: String = "",
            val uploaderUuid: String? = null
    )

    @Serializable
    data class EndData(
            val time: Long = 0,
            val offset: Double? = null,
            val deleteInstances: Boolean = false
    )
}