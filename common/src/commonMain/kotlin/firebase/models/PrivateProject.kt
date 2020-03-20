package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class PrivateProject(
        override val remoteProjectRecord: RemotePrivateProjectRecord
) : Project<ProjectType.Private>() {

    override val id = remoteProjectRecord.id

    override val remoteCustomTimes = HashMap<CustomTimeId.Private, PrivateCustomTime>()
    override val remoteTasks: MutableMap<String, Task<ProjectType.Private>>
    override val taskHierarchyContainer = TaskHierarchyContainer<ProjectType.Private>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in remoteProjectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = remoteProjectRecord.remoteTaskRecords
                .values
                .map { Task(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        remoteProjectRecord.remoteTaskHierarchyRecords
                .values
                .map { TaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }
    }

    fun newRemoteCustomTime(customTimeJson: PrivateCustomTimeJson): PrivateCustomTime {
        val remoteCustomTimeRecord = remoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: PrivateCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getRemoteCustomTime(customTimeId: CustomTimeId<*>): PrivateCustomTime {
        check(remoteCustomTimes.containsKey(customTimeId as CustomTimeId.Private))

        return remoteCustomTimes.getValue(customTimeId)
    }

    override fun getRemoteCustomTime(customTimeKey: CustomTimeKey<ProjectType.Private>): PrivateCustomTime = getRemoteCustomTime(customTimeKey.customTimeId)
    override fun getRemoteCustomTime(customTimeId: String) = getRemoteCustomTime(CustomTimeId.Private(customTimeId))

    override fun getOrCreateCustomTime(ownerKey: UserKey, customTime: Time.Custom<*>) = when (customTime) {
        is PrivateCustomTime -> customTime
        is SharedCustomTime -> {
            if (customTime.ownerKey?.toPrivateProjectKey() == id) {
                customTimes.single { it.id == customTime.privateKey }
            } else {
                val customTimeJson = PrivateCustomTimeJson(
                        customTime.name,
                        customTime.getHourMinute(DayOfWeek.SUNDAY).hour,
                        customTime.getHourMinute(DayOfWeek.SUNDAY).minute,
                        customTime.getHourMinute(DayOfWeek.MONDAY).hour,
                        customTime.getHourMinute(DayOfWeek.MONDAY).minute,
                        customTime.getHourMinute(DayOfWeek.TUESDAY).hour,
                        customTime.getHourMinute(DayOfWeek.TUESDAY).minute,
                        customTime.getHourMinute(DayOfWeek.WEDNESDAY).hour,
                        customTime.getHourMinute(DayOfWeek.WEDNESDAY).minute,
                        customTime.getHourMinute(DayOfWeek.THURSDAY).hour,
                        customTime.getHourMinute(DayOfWeek.THURSDAY).minute,
                        customTime.getHourMinute(DayOfWeek.FRIDAY).hour,
                        customTime.getHourMinute(DayOfWeek.FRIDAY).minute,
                        customTime.getHourMinute(DayOfWeek.SATURDAY).hour,
                        customTime.getHourMinute(DayOfWeek.SATURDAY).minute
                )

                newRemoteCustomTime(customTimeJson)
            }
        }
        else -> throw IllegalArgumentException()
    }
}