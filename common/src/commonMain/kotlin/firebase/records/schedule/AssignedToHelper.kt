package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.AssignedToJson
import com.krystianwsul.common.utils.ProjectType

sealed class AssignedToHelper<T : ProjectType> {

    abstract val assignedTo: Set<String>

    class Private : AssignedToHelper<ProjectType.Private>() {

        override val assignedTo = setOf<String>()
    }

    class Shared(
            private val assignedToJson: AssignedToJson,
            private val scheduleRecord: ScheduleRecord<ProjectType.Shared>,
    ) : AssignedToHelper<ProjectType.Shared>() {

        override var assignedTo
            get() = assignedToJson.assignedTo.keys
            set(value) = scheduleRecord.setProperty(assignedToJson::assignedTo, value.associateWith { true })
    }
}