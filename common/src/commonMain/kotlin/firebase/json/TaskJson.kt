package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class TaskJson @JvmOverloads constructor(
        var name: String = "",
        val startTime: Long = 0,
        var endTime: Long? = null,
        var note: String? = null,
        var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        var schedules: MutableMap<String, ScheduleWrapper> = mutableMapOf(),
        val oldestVisible: MutableMap<String, OldestVisibleJson> = mutableMapOf(), // todo oldest visible June 18th remove
        var oldestVisibleServer: String? = null, // todo oldest visible June 18th remove.  Run Irrelevant locally to check for trash being kept
        var image: Image? = null,
        var endData: EndData? = null,
        var noScheduleOrParent: Map<String, NoScheduleOrParentJson> = mutableMapOf()
) {

    @Serializable
    data class Image(
            val imageUuid: String = "",
            val uploaderUuid: String? = null
    )

    @Serializable
    data class EndData(
            val time: Long = 0,
            val deleteInstances: Boolean = false
    )
}