package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.checkme.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemotePrivateProject(
        domainFactory: DomainFactory,
        override val remoteProjectRecord: RemotePrivateProjectRecord,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject(domainFactory, uuid, now) {

    override val remoteCustomTimes = HashMap<String, RemotePrivateCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask>
    override val remoteTaskHierarchies = TaskHierarchyContainer<String, RemoteTaskHierarchy>()

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemotePrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

            if (domainFactory.localFactory.hasLocalCustomTime(remoteCustomTimeRecord.localId)) {
                val localCustomTime = domainFactory.localFactory.getLocalCustomTime(remoteCustomTimeRecord.localId)

                localCustomTime.updateRemoteCustomTimeRecord(remoteCustomTimeRecord)
            }
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(domainFactory, this, it, now) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(domainFactory, this, it) }
                .forEach { remoteTaskHierarchies.add(it.id, it) }
    }

    override fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) = throw UnsupportedOperationException()

    fun newRemoteCustomTime(customTimeJson: PrivateCustomTimeJson): RemotePrivateCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = RemotePrivateCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: RemotePrivateCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    fun getRemoteCustomTimeIfPresent(localCustomTimeId: Int) = remoteCustomTimes.values.singleOrNull { it.remoteCustomTimeRecord.let { it.ownerId == uuid && it.localId == localCustomTimeId } }
}