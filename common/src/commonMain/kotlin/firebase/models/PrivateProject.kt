package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class PrivateProject(
        override val projectRecord: PrivateProjectRecord,
        rootInstanceManagers: Map<TaskKey, RootInstanceManager<ProjectType.Private>>,
        private val _newRootInstanceManager: (TaskRecord<ProjectType.Private>) -> RootInstanceManager<ProjectType.Private>
) : Project<ProjectType.Private>() {

    override val projectKey = projectRecord.projectKey

    override val remoteCustomTimes = HashMap<CustomTimeId.Private, PrivateCustomTime>()
    override val remoteTasks: MutableMap<String, Task<ProjectType.Private>>
    override val taskHierarchyContainer = TaskHierarchyContainer<ProjectType.Private>()

    override val customTimes get() = remoteCustomTimes.values

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        remoteTasks = projectRecord.taskRecords
                .values
                .map { Task(this, it, rootInstanceManagers.getValue(it.taskKey)) }
                .associateBy { it.id }
                .toMutableMap()

        projectRecord.taskHierarchyRecords
                .values
                .map { TaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }
    }

    fun newRemoteCustomTime(customTimeJson: PrivateCustomTimeJson): PrivateCustomTime {
        val remoteCustomTimeRecord = projectRecord.newRemoteCustomTimeRecord(customTimeJson)

        val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

        check(!remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime

        return remoteCustomTime
    }

    fun deleteCustomTime(remoteCustomTime: PrivateCustomTime) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getCustomTime(customTimeId: CustomTimeId<*>): PrivateCustomTime {
        check(remoteCustomTimes.containsKey(customTimeId as CustomTimeId.Private))

        return remoteCustomTimes.getValue(customTimeId)
    }

    override fun getCustomTime(customTimeKey: CustomTimeKey<ProjectType.Private>): PrivateCustomTime = getCustomTime(customTimeKey.customTimeId)
    override fun getCustomTime(customTimeId: String) = getCustomTime(CustomTimeId.Private(customTimeId))

    override fun getOrCreateCustomTime(ownerKey: UserKey, customTime: Time.Custom<*>) = when (customTime) {
        is PrivateCustomTime -> customTime
        is SharedCustomTime -> {
            if (customTime.ownerKey?.toPrivateProjectKey() == projectKey) {
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

    override fun newRootInstanceManager(taskRecord: TaskRecord<ProjectType.Private>) = _newRootInstanceManager(taskRecord)
}