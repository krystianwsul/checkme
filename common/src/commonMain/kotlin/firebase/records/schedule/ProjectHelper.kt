package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.RootScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson

sealed class ProjectHelper {

    abstract fun getProjectId(scheduleJson: ScheduleJson): String

    abstract fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson): String

    object Project : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson): String {
            check(scheduleJson !is RootScheduleJson)

            throw UnsupportedOperationException()
        }

        override fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson): String {
            check(noScheduleOrParentJson.projectId == null)

            throw UnsupportedOperationException()
        }
    }

    object Root : ProjectHelper() {

        override fun getProjectId(scheduleJson: ScheduleJson) = (scheduleJson as RootScheduleJson).projectId

        override fun getProjectId(noScheduleOrParentJson: NoScheduleOrParentJson) = noScheduleOrParentJson.projectId!!
    }
}