package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.records.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

abstract class DatabaseWrapper {

    companion object {

        const val USERS_KEY = "users"
        const val RECORDS_KEY = "records"
        const val PRIVATE_PROJECTS_KEY = "privateProjects"
        const val KEY_INSTANCES = "instances"
    }

    protected abstract fun getNewId(path: String): String

    protected abstract fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback)

    fun getPrivateScheduleRecordId(projectId: ProjectKey<ProjectType.Private>, taskId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")

    fun getPrivateTaskRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}")

    fun getPrivateTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")

    fun getPrivateCustomTimeRecordId(projectId: ProjectKey<ProjectType.Private>) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun newSharedProjectRecordId() = ProjectKey.Shared(getNewId(RECORDS_KEY))

    fun newSharedScheduleRecordId(projectId: ProjectKey<ProjectType.Shared>, taskId: String) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")

    fun newSharedTaskRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}")

    fun newSharedTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newSharedCustomTimeRecordId(projectId: ProjectKey<ProjectType.Shared>) = getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun updateRecords(
            values: Map<String, Any?>,
            callback: DatabaseCallback
    ) = update(RECORDS_KEY, values, callback)

    fun updatePrivateProjects(
            values: Map<String, Any?>,
            callback: DatabaseCallback
    ) = update(PRIVATE_PROJECTS_KEY, values, callback)

    fun updateFriends(
            values: Map<String, Any?>,
            callback: DatabaseCallback
    ) = update(USERS_KEY, values, callback)

    fun updateInstances(
            taskFirebaseKey: String,
            values: Map<String, Any?>,
            callback: DatabaseCallback
    ) = update("$KEY_INSTANCES/$taskFirebaseKey", values, callback)
}