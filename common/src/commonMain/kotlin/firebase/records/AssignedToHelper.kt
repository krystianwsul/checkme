package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.schedule.AssignedToJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.utils.ProjectType

sealed class AssignedToHelper<T : ProjectType> {

    abstract fun getAssignedTo(scheduleJson: ScheduleJson<T>): Set<String>

    abstract fun setAssignedTo(
            assignedToJson: AssignedToJson,
            scheduleRecord: ScheduleRecord<ProjectType.Shared>,
            assignedTo: Set<String>,
    )

    object Private : AssignedToHelper<ProjectType.Private>() {

        override fun getAssignedTo(scheduleJson: ScheduleJson<ProjectType.Private>) = setOf<String>()

        override fun setAssignedTo(
                assignedToJson: AssignedToJson,
                scheduleRecord: ScheduleRecord<ProjectType.Shared>,
                assignedTo: Set<String>,
        ) = throw UnsupportedOperationException()
    }

    object Shared : AssignedToHelper<ProjectType.Shared>() {

        override fun getAssignedTo(scheduleJson: ScheduleJson<ProjectType.Shared>) = (scheduleJson as AssignedToJson).assignedTo.keys

        override fun setAssignedTo(
                assignedToJson: AssignedToJson,
                scheduleRecord: ScheduleRecord<ProjectType.Shared>,
                assignedTo: Set<String>,
        ) = scheduleRecord.setProperty(assignedToJson::assignedTo, assignedTo.associateWith { true })
    }
}