package com.krystianwsul.checkme.domainmodel.local

import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.checkme.persistencemodel.TaskHierarchyRecord
import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class LocalTaskHierarchy(
        kotlinDomainFactory: KotlinDomainFactory,
        private val taskHierarchyRecord: TaskHierarchyRecord) : TaskHierarchy(kotlinDomainFactory) {

    val id get() = taskHierarchyRecord.id

    override val startExactTimeStamp get() = ExactTimeStamp(taskHierarchyRecord.startTime)

    override val parentTaskKey get() = TaskKey(parentTaskId)

    override val childTaskKey get() = TaskKey(childTaskId)

    val parentTaskId get() = taskHierarchyRecord.parentTaskId

    val childTaskId get() = taskHierarchyRecord.childTaskId

    override val parentTask get() = kotlinDomainFactory.localFactory.getTaskForce(parentTaskId)

    override val childTask get() = kotlinDomainFactory.localFactory.getTaskForce(childTaskId)

    override var ordinal: Double
        get() = taskHierarchyRecord.ordinal ?: taskHierarchyRecord.startTime.toDouble()
        set(value) {
            taskHierarchyRecord.ordinal = value
        }

    override val taskHierarchyKey get() = TaskHierarchyKey.LocalTaskHierarchyKey(taskHierarchyRecord.id)

    public override fun getEndExactTimeStamp() = taskHierarchyRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setEndExactTimeStamp(now: ExactTimeStamp) {
        check(current(now))

        taskHierarchyRecord.endTime = now.long
    }

    override fun delete() {
        kotlinDomainFactory.localFactory.deleteTaskHierarchy(this)

        taskHierarchyRecord.delete()
    }
}
