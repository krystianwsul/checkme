package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.RootScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson

sealed class ProjectHelper {

    abstract fun getProjectId(scheduleJson: ScheduleJson): String

    abstract fun setProjectId(
            scheduleJson: ScheduleJson,
            projectId: String,
            addValue: (subKey: String, value: String) -> Unit,
    )

    object Project : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson): String {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }

        override fun setProjectId(
                scheduleJson: ScheduleJson,
                projectId: String,
                addValue: (subKey: String, value: String) -> Unit,
        ) {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }
    }

    object Root : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson) = (scheduleJson as RootScheduleJson).projectId

        override fun setProjectId(
                scheduleJson: ScheduleJson,
                projectId: String,
                addValue: (subKey: String, String) -> Unit,
        ) {
            if ((scheduleJson as RootScheduleJson).projectId == projectId) return

            scheduleJson.projectId = projectId

            addValue("projectId", projectId)
        }
    }
}