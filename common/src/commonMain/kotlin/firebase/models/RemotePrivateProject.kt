package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateProject(
        override val remoteProjectRecord: RemotePrivateProjectRecord,
        uuid: String
) : RemoteProject<RemoteCustomTimeId.Private>(uuid) {

    override val remoteCustomTimes = HashMap<RemoteCustomTimeId.Private, RemotePrivateCustomTime>()
    override val remoteTasks: MutableMap<String, RemoteTask<RemoteCustomTimeId.Private>>
    override val remoteTaskHierarchyContainer = TaskHierarchyContainer<String, RemoteTaskHierarchy<RemoteCustomTimeId.Private>>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.remoteCustomTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = RemotePrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { RemoteTask(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { RemoteTaskHierarchy(this, it) }
                .forEach { remoteTaskHierarchyContainer.add(it.id, it) }
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

    override fun getRemoteCustomTime(remoteCustomTimeId: RemoteCustomTimeId): RemotePrivateCustomTime {
        check(remoteCustomTimes.containsKey(remoteCustomTimeId))

        return remoteCustomTimes.getValue(remoteCustomTimeId as RemoteCustomTimeId.Private)
    }

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)

    override fun getOrCreateCustomTime(ownerKey: String, remoteCustomTime: RemoteCustomTime<*>) = when (remoteCustomTime) {
        is RemotePrivateCustomTime -> remoteCustomTime
        is RemoteSharedCustomTime -> {
            if (remoteCustomTime.ownerKey == id) {
                customTimes.single { it.id == remoteCustomTime.privateKey }
            } else {
                val customTimeJson = PrivateCustomTimeJson(
                        remoteCustomTime.name,
                        remoteCustomTime.getHourMinute(DayOfWeek.SUNDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.SUNDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.MONDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.MONDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.TUESDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.TUESDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.THURSDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.THURSDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.FRIDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.FRIDAY).minute,
                        remoteCustomTime.getHourMinute(DayOfWeek.SATURDAY).hour,
                        remoteCustomTime.getHourMinute(DayOfWeek.SATURDAY).minute
                )

                newRemoteCustomTime(customTimeJson)
            }
        }
        else -> throw IllegalArgumentException()
    }
}