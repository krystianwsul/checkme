package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.RootScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.utils.ProjectKey

sealed class ProjectHelper {

    abstract fun getProjectId(scheduleJson: ScheduleJson): String

    abstract fun getProjectKey(scheduleJson: ScheduleJson): ProjectKey<*>?

    abstract fun setProjectKey(
        scheduleJson: ScheduleJson,
        projectKey: ProjectKey<*>,
        addValue: (subKey: String, value: String) -> Unit,
    )

    object Project : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson): String {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }

        override fun getProjectKey(scheduleJson: ScheduleJson): ProjectKey<*>? {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }

        override fun setProjectKey(
            scheduleJson: ScheduleJson,
            projectKey: ProjectKey<*>,
            addValue: (subKey: String, value: String) -> Unit,
        ) {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }
    }

    object Root : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson) = (scheduleJson as RootScheduleJson).projectId

        override fun getProjectKey(scheduleJson: ScheduleJson) = scheduleJson.let { it as RootScheduleJson }
            .projectKey
            ?.let { ProjectKey.fromJson(it) }

        override fun setProjectKey(
            scheduleJson: ScheduleJson,
            projectKey: ProjectKey<*>,
            addValue: (subKey: String, String) -> Unit,
        ) {
            check(scheduleJson is RootScheduleJson)

            val projectId = projectKey.key
            if (scheduleJson.projectId != projectId) {
                scheduleJson.projectId = projectId
                addValue("projectId", projectId)
            }

            val projectKeyJson = projectKey.toJson()
            if (scheduleJson.projectKey != projectKeyJson) {
                scheduleJson.projectKey = projectKeyJson
                addValue("projectKey", projectKeyJson)
            }
        }
    }
}