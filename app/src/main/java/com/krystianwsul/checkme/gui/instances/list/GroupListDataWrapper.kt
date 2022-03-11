package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.MixedInstanceDataCollection
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.utils.FilterParamsMatchable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import java.util.*

data class GroupListDataWrapper(
    val customTimeDatas: List<CustomTimeData>,
    val taskEditable: Boolean?,
    val taskDatas: List<TaskData>,
    val note: String?,
    val mixedInstanceDataCollection: MixedInstanceDataCollection,
    val doneSingleBridges: List<GroupTypeFactory.SingleBridge>,
    val imageData: ImageState?,
    val projectInfo: DetailsNode.ProjectInfo?,
    val dropParent: DropParent,
) {

    val allInstanceDatas get() = mixedInstanceDataCollection.instanceDatas + doneSingleBridges.map { it.instanceData }

    data class TaskData(
        override val taskKey: TaskKey,
        override val name: String,
        val children: List<TaskData>,
        override val note: String?,
        val imageState: ImageState?,
        override val isAssignedToMe: Boolean,
        val projectInfo: DetailsNode.ProjectInfo?,
        val ordinal: Ordinal,
        override val canMigrateDescription: Boolean,
    ) : SelectedData, QueryMatchable, FilterParamsMatchable {

        init {
            check(name.isNotEmpty())
        }

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override val taskCurrent = true
        override val canAddSubtask = true

        override val childSelectedDatas get() = children
    }

    /*
    The next time you want to add something here, consider these options:

    1. For location-dependent properties of instance, consider putting it in SingleBridge.
    2. If for whatever reason you need a separate class, consider making new InstanceMetadata class that's created inside
    SingleBridge.
     */
    data class InstanceData(
        val done: ExactTimeStamp.Local?,
        val instanceKey: InstanceKey,
        override val name: String,
        val instanceTimeStamp: TimeStamp,
        val instanceDate: Date,
        override val taskCurrent: Boolean,
        override val canAddSubtask: Boolean,
        override val canMigrateDescription: Boolean,
        val createTaskTimePair: TimePair,
        override val note: String?,
        val mixedInstanceDataCollection: MixedInstanceDataCollection,
        val doneSingleBridges: List<GroupTypeFactory.SingleBridge>,
        val ordinal: Ordinal,
        val imageState: ImageState?,
        override val isAssignedToMe: Boolean,
        val projectKey: ProjectKey.Shared?,
        val parentInstanceKey: InstanceKey?,
    ) : Comparable<InstanceData>, SelectedData, QueryMatchable, FilterParamsMatchable {

        companion object

        val isRootInstance = parentInstanceKey == null

        val allChildren = mixedInstanceDataCollection.instanceDatas + doneSingleBridges.map { it.instanceData }

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        init {
            check(name.isNotEmpty())
        }

        // this is valid only for top-level instances
        override fun compareTo(other: InstanceData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0) return timeStampComparison

            return ordinal.compareTo(other.ordinal)
        }

        override val taskKey = instanceKey.taskKey

        override val childSelectedDatas get() = allChildren

        fun normalize() {
            normalizedFields
        }
    }

    interface SelectedData {

        val taskCurrent: Boolean
        val canAddSubtask: Boolean
        val canMigrateDescription: Boolean
        val taskKey: TaskKey
        val name: String
        val note: String?
        val childSelectedDatas: Collection<SelectedData>
    }

    data class CustomTimeData(val name: String, val hourMinutes: SortedMap<DayOfWeek, HourMinute>) {

        init {
            check(name.isNotEmpty())
            check(hourMinutes.size == 7)
        }
    }
}