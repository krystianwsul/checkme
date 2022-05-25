package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.schedule.SharedScheduleWrapper
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ScheduleId

class SharedTaskRecord(
    id: String,
    sharedProjectRecord: SharedOwnedProjectRecord,
    private val taskJson: SharedTaskJson,
) : ProjectTaskRecord(false, id, sharedProjectRecord, taskJson, AssignedToHelper.Shared) {

    override val createObject: SharedTaskJson // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = instanceRecords.entries
                    .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                    .toMutableMap()

            val scheduleWrappers = HashMap<ScheduleId, SharedScheduleWrapper>()

            for (singleScheduleRecord in singleScheduleRecords.values)
                scheduleWrappers[singleScheduleRecord.id] = singleScheduleRecord.createObject as SharedScheduleWrapper

            for (weeklyScheduleRecord in weeklyScheduleRecords.values)
                scheduleWrappers[weeklyScheduleRecord.id] = weeklyScheduleRecord.createObject as SharedScheduleWrapper

            for (monthlyDayScheduleRecord in monthlyDayScheduleRecords.values)
                scheduleWrappers[monthlyDayScheduleRecord.id] =
                    monthlyDayScheduleRecord.createObject as SharedScheduleWrapper

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] =
                    monthlyWeekScheduleRecord.createObject as SharedScheduleWrapper

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject as SharedScheduleWrapper

            taskJson.schedules = scheduleWrappers.mapKeys { it.key.value }.toMutableMap()

            taskJson.noScheduleOrParent = noScheduleOrParentRecords.mapValues { it.value.createObject }

            taskJson.taskHierarchies = taskHierarchyRecords.values.associate { it.id.value to it.createObject }

            return taskJson
        }

    override var startTimeOffset by Committer(taskJson::startTimeOffset)
}
