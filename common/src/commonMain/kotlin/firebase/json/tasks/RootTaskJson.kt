package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.json.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.RootTaskParentJson
import com.krystianwsul.common.firebase.json.schedule.RootScheduleWrapper
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class RootTaskJson @JvmOverloads constructor(
    override var name: String = "",
    override val startTime: Long = 0,
    override val startTimeOffset: Double = 0.0,
    override var note: String? = null,
    override var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
    override var schedules: MutableMap<String, RootScheduleWrapper> = mutableMapOf(),
    override var image: TaskJson.Image? = null,
    override var endData: TaskJson.EndData? = null,
    override var noScheduleOrParent: Map<String, RootNoScheduleOrParentJson> = mapOf(),
    override var ordinal: Double? = null,
    override var taskHierarchies: Map<String, NestedTaskHierarchyJson> = mapOf(),
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : TaskJson, Parsable, RootTaskParentJson