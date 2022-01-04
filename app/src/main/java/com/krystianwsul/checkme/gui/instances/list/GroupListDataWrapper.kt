package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.domainmodel.MixedInstanceDataCollection
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.utils.FilterParamsMatchable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.normalized
import java.util.*

data class GroupListDataWrapper(
    val customTimeDatas: List<CustomTimeData>,
    val taskEditable: Boolean?,
    val taskDatas: List<TaskData>,
    val note: String?,
    val mixedInstanceDataCollection: MixedInstanceDataCollection,
    val doneInstanceDatas: List<InstanceData>,
    val imageData: ImageState?,
    val projectInfo: DetailsNode.ProjectInfo?,
) {

    val allInstanceDatas get() = mixedInstanceDataCollection.instanceDatas + doneInstanceDatas

    data class TaskData(
        override val taskKey: TaskKey,
        override val name: String,
        val children: List<TaskData>,
        override val note: String?,
        val imageState: ImageState?,
        override val isAssignedToMe: Boolean,
        val projectInfo: DetailsNode.ProjectInfo?,
        val ordinal: Double,
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

    data class InstanceData(
        val done: ExactTimeStamp.Local?,
        val instanceKey: InstanceKey,
        val displayText: String?,
        override val name: String,
        val instanceTimeStamp: TimeStamp,
        val instanceDate: Date,
        override val taskCurrent: Boolean,
        override val canAddSubtask: Boolean,
        override val canMigrateDescription: Boolean,
        val isRootInstance: Boolean,
        val createTaskTimePair: TimePair,
        override val note: String?,
        val children: MixedInstanceDataCollection,
        val ordinal: Double,
        val notificationShown: Boolean,
        val imageState: ImageState?,
        override val isAssignedToMe: Boolean,
        val projectInfo: DetailsNode.ProjectInfo?, // this is for what's displayed
        val projectKey: ProjectKey.Shared? // this is for creating new tasks via ActionMode.  Always set
    ) : Comparable<InstanceData>, SelectedData, QueryMatchable, FilterParamsMatchable {

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

        override val childSelectedDatas get() = children.instanceDatas

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