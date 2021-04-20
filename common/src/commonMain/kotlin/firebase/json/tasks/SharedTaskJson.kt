package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.SharedScheduleWrapper
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedTaskJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var note: String? = null,
        override var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        override var schedules: MutableMap<String, SharedScheduleWrapper> = mutableMapOf(),
        override var image: TaskJson.Image? = null,
        override var endData: TaskJson.EndData? = null,
        override var noScheduleOrParent: Map<String, NoScheduleOrParentJson> = mutableMapOf(),
        override var ordinal: Double? = null,
        override var taskHierarchies: Map<String, NestedTaskHierarchyJson> = mapOf(),
) : TaskJson