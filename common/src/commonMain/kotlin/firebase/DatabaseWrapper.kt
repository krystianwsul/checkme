package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.records.customtime.CustomTimeRecord
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskHierarchyId
import com.krystianwsul.common.utils.UserKey

abstract class DatabaseWrapper {

    companion object {

        const val USERS_KEY = "users"
        const val RECORDS_KEY = "records"
        const val PRIVATE_PROJECTS_KEY = "privateProjects"
        const val TASKS_KEY = "tasks"
    }

    protected abstract fun getNewId(path: String): String

    abstract fun update(values: Map<String, Any?>, callback: DatabaseCallback)

    fun newPrivateProjectTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Private>) =
        TaskHierarchyId(getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskHierarchyRecord.TASK_HIERARCHIES}"))

    fun newPrivateNestedTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Private>, taskId: String) =
        TaskHierarchyId(getNewId("$PRIVATE_PROJECTS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}"))

    fun newRootUserCustomTimeId(userKey: UserKey) = getNewId("$USERS_KEY/${userKey.key}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun newSharedProjectRecordId() = ProjectKey.Shared(getNewId(RECORDS_KEY))

    fun newSharedNestedTaskHierarchyRecordId(projectId: ProjectKey<ProjectType.Shared>, taskId: String) =
        TaskHierarchyId(getNewId("$RECORDS_KEY/$projectId/${ProjectRecord.PROJECT_JSON}/${TaskRecord.TASKS}/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}"))

    // root tasks

    fun newRootTaskRecordId() = getNewId(TASKS_KEY)

    fun newRootTaskScheduleRecordId(taskId: String) = getNewId("$TASKS_KEY/$taskId/${ScheduleRecord.SCHEDULES}")

    fun newRootTaskNoScheduleOrParentRecordId(taskId: String) =
        getNewId("$TASKS_KEY/$taskId/${NoScheduleOrParentRecord.NO_SCHEDULE_OR_PARENT}")

    fun newRootTaskNestedTaskHierarchyRecordId(taskId: String) =
        TaskHierarchyId(getNewId("$TASKS_KEY/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}"))
}