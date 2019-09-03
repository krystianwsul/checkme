package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.firebase.PrivateCustomTimeJson
import com.krystianwsul.checkme.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemotePrivateProject(
        domainFactory: DomainFactory,
        override val remoteProjectRecord: RemotePrivateProjectRecord,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject<RemoteCustomTimeId.Private>(domainFactory, uuid) {

    override val remoteCustomTimes = HashMap<RemoteCustomTimeId.Private, RemotePrivateCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask<RemoteCustomTimeId.Private>>
    override val remoteTaskHierarchyContainer = TaskHierarchyContainer<String, RemoteTaskHierarchy<RemoteCustomTimeId.Private>>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemotePrivateCustomTime(domainFactory, this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(domainFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(domainFactory, this, it) }
                .forEach { remoteTaskHierarchyContainer.add(it.id, it) }
    }

    override fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) = throw UnsupportedOperationException()

    fun newRemoteCustomTime(customTimeJson: com.krystianwsul.common.firebase.PrivateCustomTimeJson): RemotePrivateCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = RemotePrivateCustomTime(domainFactory, this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: RemotePrivateCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getRemoteCustomTimeKey(customTimeKey: CustomTimeKey<*>): CustomTimeKey.Private = when (customTimeKey) {
        is CustomTimeKey.Private -> customTimeKey
        is CustomTimeKey.Shared -> throw UnsupportedOperationException()
    }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemotePrivateCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Private)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)
}