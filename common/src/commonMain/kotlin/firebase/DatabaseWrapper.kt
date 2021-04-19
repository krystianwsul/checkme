package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.records.*
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

abstract class DatabaseWrapper {

    companion object {

        const val USERS_KEY = "users"
        const val RECORDS_KEY = "records"
        const val PRIVATE_PROJECTS_KEY = "privateProjects"
        const val KEY_INSTANCES = "instances"
    }

    protected abstract fun getNewId(path: String): String

    abstract fun update(values: Map<String, Any?>, callback: DatabaseCallback)

    fun newPrivateScheduleRecordId(projectId: ProjectKey<ProjectType.Private>, taskId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${ScheduleRecord.SCHEDULES}")

    fun newPrivateNoScheduleOrParentRecordId(projectId: ProjectKey<ProjectType.Private>, taskId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${NoScheduleOrParentRecord.NO_SCHEDULE_OR_PARENT}")

    fun newPrivateTaskRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}")

    fun newPrivateProjectTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newPrivateNestedTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Private>, taskId: String) =
            getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newPrivateCustomTimeRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun newRootUserCustomTimeId(userKey: UserKey) = getNewId("$USERS_KEY/${userKey.key}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun newSharedProjectRecordId() = ProjectKey.Shared(getNewId(RECORDS_KEY))

    fun newSharedScheduleRecordId(projectId: ProjectKey<ProjectType.Shared>, taskId: String) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${ScheduleRecord.SCHEDULES}")

    fun newSharedNoScheduleOrParentRecordId(projectId: ProjectKey<ProjectType.Shared>, taskId: String) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${NoScheduleOrParentRecord.NO_SCHEDULE_OR_PARENT}")

    fun newSharedTaskRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}")

    fun newSharedProjectTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newSharedNestedTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Shared>, taskId: String) =
            getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newSharedCustomTimeRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${CustomTimeRecord.CUSTOM_TIMES}")
}