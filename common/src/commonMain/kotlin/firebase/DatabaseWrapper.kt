package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.records.*

abstract class DatabaseWrapper {

    companion object {

        const val USERS_KEY = "users"
        const val RECORDS_KEY = "records"
        const val PRIVATE_PROJECTS_KEY = "privateProjects"

        lateinit var instance: DatabaseWrapper
    }

    protected abstract fun getNewId(path: String): String

    fun getPrivateScheduleRecordId(projectId: String, taskId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")

    fun getPrivateTaskRecordId(projectId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}")

    fun getPrivateTaskHierarchyRecordId(projectId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")

    fun getPrivateCustomTimeRecordId(projectId: String) = getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteCustomTimeRecord.CUSTOM_TIMES}")

    fun newSharedProjectRecordId() = getNewId(RECORDS_KEY)

    fun newSharedScheduleRecordId(projectId: String, taskId: String) = getNewId("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")

    fun newSharedTaskRecordId(projectId: String) = getNewId("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}")

    fun newSharedTaskHierarchyRecordId(projectId: String) = getNewId("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")

    fun newSharedCustomTimeRecordId(projectId: String) = getNewId("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteCustomTimeRecord.CUSTOM_TIMES}")

    abstract fun updateRecords(values: Map<String, Any?>, callback: DatabaseCallback)

    abstract fun updatePrivateProject(values: Map<String, Any?>, callback: DatabaseCallback)
}