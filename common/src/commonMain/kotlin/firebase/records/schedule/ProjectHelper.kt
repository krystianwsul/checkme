package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.RootScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson

sealed class ProjectHelper {

    abstract fun getProjectId(scheduleJson: ScheduleJson): String

    abstract fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson): String

    abstract fun setProjectId(
            scheduleJson: ScheduleJson,
            projectId: String,
            addValue: (subKey: String, value: String) -> Unit,
    )

    abstract fun setProjectId(
        noScheduleOrParentJson: NoScheduleOrParentJson,
        projectId: String,
        addValue: (subKey: String, value: String) -> Unit,
    )

    object Project : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson): String {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }

        override fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson): String {
            check(noScheduleOrParentJson.projectId == null)

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

        override fun setProjectId(
            noScheduleOrParentJson: NoScheduleOrParentJson,
            projectId: String,
            addValue: (subKey: String, value: String) -> Unit,
        ) {
            check(noScheduleOrParentJson.projectId == null)

            throw UnsupportedOperationException()
        }
    }

    object Root : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson) = (scheduleJson as RootScheduleJson).projectId

        override fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson) = noScheduleOrParentJson.projectId!!

        override fun setProjectId(
                scheduleJson: ScheduleJson,
                projectId: String,
                addValue: (subKey: String, String) -> Unit,
        ) {
            if ((scheduleJson as RootScheduleJson).projectId == projectId) return

            scheduleJson.projectId = projectId

            addValue("projectId", projectId)
        }

        override fun setProjectId(
            noScheduleOrParentJson: NoScheduleOrParentJson,
            projectId: String,
            addValue: (subKey: String, String) -> Unit,
        ) {
            if (noScheduleOrParentJson.projectId == projectId) return

            noScheduleOrParentJson.projectId = projectId

            addValue("projectId", projectId)
        }
    }
}