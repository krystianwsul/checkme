package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.utils.FilterParamsMatchable
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.normalized
import java.util.*

data class GroupListDataWrapper(
        val customTimeDatas: List<CustomTimeData>,
        val taskEditable: Boolean?,
        val taskDatas: List<TaskData>,
        val note: String?,
        val instanceDatas: List<InstanceData>,
        val imageData: ImageState?,
        val projectInfo: DetailsNode.ProjectInfo?,
) : InstanceDataParent {

    data class TaskData(
            override val taskKey: TaskKey,
            override val name: String,
            val children: List<TaskData>,
            val startExactTimeStamp: ExactTimeStamp.Local,
            override val note: String?,
            val imageState: ImageState?,
            override val isAssignedToMe: Boolean,
    ) : SelectedData, QueryMatchable, FilterParamsMatchable {

        init {
            check(name.isNotEmpty())
        }

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override val taskCurrent = true
        override val taskVisible = true

        override val childSelectedDatas get() = children
    }

    data class InstanceData(
            var done: ExactTimeStamp.Local?,
            val instanceKey: InstanceKey,
            var displayText: String?,
            override val name: String,
            val instanceTimeStamp: TimeStamp,
            val instanceDateTime: DateTime,
            override var taskCurrent: Boolean,
            override val taskVisible: Boolean,
            val isRootInstance: Boolean,
            var isRootTask: Boolean?,
            var exists: Boolean,
            val createTaskTimePair: TimePair,
            override val note: String?,
            val children: MutableMap<InstanceKey, InstanceData>,
            var ordinal: Double,
            var notificationShown: Boolean,
            val imageState: ImageState?,
            val isRecurringGroupChild: Boolean,
            override val isAssignedToMe: Boolean,
            val projectInfo: DetailsNode.ProjectInfo?,
    ) : InstanceDataParent, Comparable<InstanceData>, SelectedData, QueryMatchable, FilterParamsMatchable {

        lateinit var instanceDataParent: InstanceDataParent

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        val colorEnabled = taskCurrent && isAssignedToMe

        init {
            check(name.isNotEmpty())
        }

        override fun compareTo(other: InstanceData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0)
                return timeStampComparison

            return ordinal.compareTo(other.ordinal)
        }

        override val taskKey = instanceKey.taskKey

        override val childSelectedDatas get() = children.values

        fun normalize() {
            normalizedFields
        }
    }

    interface SelectedData {

        val taskCurrent: Boolean
        val taskVisible: Boolean
        val taskKey: TaskKey
        val name: String
        val note: String?
        val childSelectedDatas: Collection<SelectedData>
    }

    data class CustomTimeData(val Name: String, val HourMinutes: SortedMap<DayOfWeek, HourMinute>) {

        init {
            check(Name.isNotEmpty())
            check(HourMinutes.size == 7)
        }
    }
}