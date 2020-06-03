package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.HierarchyData
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import java.util.*

data class GroupListDataWrapper(
        val customTimeDatas: List<CustomTimeData>,
        val taskEditable: Boolean?,
        val taskDatas: List<TaskData>,
        val note: String?,
        val instanceDatas: List<InstanceData>,
        val imageData: ImageState?
) : InstanceDataParent {

    data class TaskData(
            override val taskKey: TaskKey,
            override val name: String,
            val children: List<TaskData>,
            val startExactTimeStamp: ExactTimeStamp,
            override val note: String?,
            val imageState: ImageState?
    ) : SelectedData {

        init {
            check(name.isNotEmpty())
        }

        override val taskCurrent = true

        override val childSelectedDatas get() = children
    }

    data class InstanceData(
            var done: ExactTimeStamp?,
            val instanceKey: InstanceKey,
            var displayText: String?,
            override val name: String,
            var instanceTimeStamp: TimeStamp,
            override var taskCurrent: Boolean,
            val isRootInstance: Boolean,
            var isRootTask: Boolean?,
            var exists: Boolean,
            val createTaskTimePair: TimePair,
            override val note: String?,
            val children: MutableMap<InstanceKey, InstanceData>,
            val hierarchyData: HierarchyData?,
            var ordinal: Double,
            var notificationShown: Boolean,
            val imageState: ImageState?,
            val isRecurringGroupChild: Boolean
    ) : InstanceDataParent, Comparable<InstanceData>, SelectedData {

        lateinit var instanceDataParent: InstanceDataParent

        init {
            check(name.isNotEmpty())
        }

        override fun compareTo(other: InstanceData): Int {
            check(this::instanceDataParent.isInitialized) // sanity check

            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0)
                return timeStampComparison

            return if (hierarchyData != null) {
                checkNotNull(other.hierarchyData)

                hierarchyData.ordinal.compareTo(other.hierarchyData.ordinal)
            } else {
                check(other.hierarchyData == null)

                ordinal.compareTo(other.ordinal)
            }
        }

        override val taskKey = instanceKey.taskKey

        override val childSelectedDatas get() = children.values
    }

    interface SelectedData {

        val taskCurrent: Boolean
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