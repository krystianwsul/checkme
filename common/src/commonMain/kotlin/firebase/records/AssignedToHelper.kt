package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.schedule.AssignedToJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.WriteAssignedToJson
import com.krystianwsul.common.firebase.records.schedule.SingleScheduleRecord
import com.krystianwsul.common.utils.ProjectType

sealed class AssignedToHelper<T : ProjectType> {

    abstract fun getAssignedTo(scheduleJson: ScheduleJson): Set<String>

    abstract fun setAssignedTo(
            assignedToJson: WriteAssignedToJson,
            singleScheduleRecord: SingleScheduleRecord,
            assignedTo: Set<String>,
    )

    object Private : AssignedToHelper<ProjectType.Private>() {

        override fun getAssignedTo(scheduleJson: ScheduleJson) = setOf<String>()

        override fun setAssignedTo(
                assignedToJson: WriteAssignedToJson,
                singleScheduleRecord: SingleScheduleRecord,
                assignedTo: Set<String>,
        ) = throw UnsupportedOperationException()
    }

    object Shared : AssignedToHelper<ProjectType.Shared>() {

        override fun getAssignedTo(scheduleJson: ScheduleJson) = (scheduleJson as AssignedToJson).assignedTo.keys

        override fun setAssignedTo(
                assignedToJson: WriteAssignedToJson,
                singleScheduleRecord: SingleScheduleRecord,
                assignedTo: Set<String>,
        ) = singleScheduleRecord.setProperty(
                assignedToJson::assignedTo,
                assignedTo.associateWith { true },
                singleScheduleRecord.keyPlusSubkey,
        )
    }
}