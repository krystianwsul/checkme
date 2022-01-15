package com.krystianwsul.checkme.gui.instances.list

import arrow.optics.optics
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

    @optics
    data class InstanceData(
        val done: ExactTimeStamp.Local?,
        val instanceKey: InstanceKey,
        val displayText: String?, // todo display
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
        val projectInfo: DetailsNode.ProjectInfo?, // this is for what's displayed
        val projectKey: ProjectKey.Shared?, // this is for creating new tasks via ActionMode.  Always set
        val parentInstanceKey: InstanceKey?,
    ) : Comparable<InstanceData>, SelectedData, QueryMatchable, FilterParamsMatchable {

        companion object

        val isRootInstance = parentInstanceKey == null

        val allChildren = mixedInstanceDataCollection.instanceDatas + doneSingleBridges.map { it.instanceData }

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        init {
            check(name.isNotEmpty())
        }

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

        fun stripProjectDetails() = Companion.projectInfo.modify(this) { it.copy(projectDetails = null) }
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