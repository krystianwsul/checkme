package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.schedule.PrivateScheduleWrapper
import com.krystianwsul.common.utils.ProjectType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateTaskJson @JvmOverloads constructor(
        // todo taskhierarchy copy?
        override var name: String = "",
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var note: String? = null,
        override var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        override var schedules: MutableMap<String, PrivateScheduleWrapper> = mutableMapOf(),
        override var image: TaskJson.Image? = null,
        override var endData: TaskJson.EndData? = null,
        override var noScheduleOrParent: Map<String, NoScheduleOrParentJson> = mutableMapOf(),
        override var ordinal: Double? = null,
        override val taskHierarchies: Map<String, NestedTaskHierarchyJson> = mapOf(),
) : TaskJson<ProjectType.Private>