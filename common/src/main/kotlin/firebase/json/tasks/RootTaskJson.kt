package com.krystianwsul.common.firebase.json.tasks

import com.krystianwsul.common.firebase.json.DeepCopy
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.json.RootTaskParentJson
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.RootScheduleWrapper
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import kotlinx.serialization.Serializable

@Serializable
data class RootTaskJson @JvmOverloads constructor(
    override var name: String = "",
    override val startTime: Long = 0,
    override val startTimeOffset: Double = 0.0,
    override var note: String? = null,
    override var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
    override var schedules: MutableMap<String, RootScheduleWrapper> = mutableMapOf(),
    override var image: TaskJson.Image? = null,
    var endData: EndData? = null,
    override var noScheduleOrParent: Map<String, RootNoScheduleOrParentJson> = mapOf(),
    override var ordinal: Double? = null,
    override var ordinalString: String? = null,
    override var taskHierarchies: Map<String, NestedTaskHierarchyJson> = mapOf(),
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : TaskJson, Parsable, RootTaskParentJson, DeepCopy<RootTaskJson> {

    override fun deepCopy() = deepCopy(serializer(), this)

    @Serializable
    data class EndData(
        override val time: Long = 0,
        override val offset: Double = 0.0,
        override val deleteInstances: Boolean = false,
    ) : TaskJson.EndData {

        fun toCompat() = ProjectTaskJson.EndData(time, offset, deleteInstances)
    }
}