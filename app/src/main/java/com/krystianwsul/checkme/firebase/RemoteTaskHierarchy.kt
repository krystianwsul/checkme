package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord
import com.krystianwsul.checkme.utils.TaskHierarchyKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp


class RemoteTaskHierarchy(kotlinDomainFactory: KotlinDomainFactory, private val remoteProject: RemoteProject, private val remoteTaskHierarchyRecord: RemoteTaskHierarchyRecord) : TaskHierarchy(kotlinDomainFactory) {

    override val startExactTimeStamp get() = ExactTimeStamp(remoteTaskHierarchyRecord.startTime)

    override val parentTaskKey by lazy { TaskKey(remoteProject.id, remoteTaskHierarchyRecord.parentTaskId) }

    override val childTaskKey by lazy { TaskKey(remoteProject.id, remoteTaskHierarchyRecord.childTaskId) }

    val id by lazy { remoteTaskHierarchyRecord.id }

    override val parentTask by lazy { remoteProject.getRemoteTaskForce(parentTaskId) }

    override val childTask by lazy { remoteProject.getRemoteTaskForce(childTaskId) }

    private val parentTaskId by lazy { remoteTaskHierarchyRecord.parentTaskId }

    private val childTaskId by lazy { remoteTaskHierarchyRecord.childTaskId }

    override var ordinal: Double
        get() = remoteTaskHierarchyRecord.ordinal ?: remoteTaskHierarchyRecord.startTime.toDouble()
        set(ordinal) = remoteTaskHierarchyRecord.setOrdinal(ordinal)

    override val taskHierarchyKey by lazy { TaskHierarchyKey.RemoteTaskHierarchyKey(remoteProject.id, remoteTaskHierarchyRecord.id) }

    public override fun getEndExactTimeStamp() = remoteTaskHierarchyRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setEndExactTimeStamp(now: ExactTimeStamp) {
        check(current(now))

        remoteTaskHierarchyRecord.setEndTime(now.long)
    }

    override fun delete() {
        remoteProject.deleteTaskHierarchy(this)

        remoteTaskHierarchyRecord.delete()
    }
}
