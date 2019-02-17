package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.checkme.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.TaskHierarchyContainer
import com.krystianwsul.checkme.utils.time.DayOfWeek
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

        domainFactory.localFactory
                .localCustomTimes
                .filter { getRemoteCustomTimeIfPresent(it.id) == null }
                .forEach { getRemoteCustomTimeKey(it.customTimeKey) }
    }

    override fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) = throw UnsupportedOperationException()

    private fun newRemoteCustomTime(customTimeJson: PrivateCustomTimeJson): RemotePrivateCustomTime {
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

    override fun getRemoteCustomTimeIfPresent(localCustomTimeId: Int) = remoteCustomTimes.values.singleOrNull { it.remoteCustomTimeRecord.localId == localCustomTimeId }

    override fun getRemoteCustomTimeKey(customTimeKey: CustomTimeKey): CustomTimeKey.RemoteCustomTimeKey<RemoteCustomTimeId.Private> = when (customTimeKey) {
        is CustomTimeKey.LocalCustomTimeKey -> {
            val localCustomTimeId = customTimeKey.localCustomTimeId

            val localCustomTime = domainFactory.localFactory.getLocalCustomTime(localCustomTimeId)

            val remoteCustomTime = getRemoteCustomTimeIfPresent(localCustomTimeId)
            if (remoteCustomTime != null) {
                remoteCustomTime.customTimeKey
            } else {
                val customTimeJson = PrivateCustomTimeJson(localCustomTime.id, localCustomTime.name, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute, localCustomTime.getHourMinute(DayOfWeek.MONDAY).hour, localCustomTime.getHourMinute(DayOfWeek.MONDAY).minute, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour, localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour, localCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour, localCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour, localCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute, localCustomTime.current)

                newRemoteCustomTime(customTimeJson).customTimeKey
            }
        }
        is CustomTimeKey.RemoteCustomTimeKey<*> -> {
            when (customTimeKey.remoteCustomTimeId) {
                is RemoteCustomTimeId.Private -> customTimeKey as CustomTimeKey.RemoteCustomTimeKey<RemoteCustomTimeId.Private>
                is RemoteCustomTimeId.Shared -> throw UnsupportedOperationException()
            }
        }
    }

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemotePrivateCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Private)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)
}