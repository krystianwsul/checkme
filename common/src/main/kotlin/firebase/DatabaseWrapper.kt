package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.records.customtime.CustomTimeRecord
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.schedule.ScheduleRecord
import com.krystianwsul.common.firebase.records.taskhierarchy.TaskHierarchyRecord
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.utils.*

abstract class DatabaseWrapper {

    companion object {

        const val USERS_KEY = "users"
        const val RECORDS_KEY = "records"
        const val PRIVATE_PROJECTS_KEY = "privateProjects"
        const val TASKS_KEY = "tasks"
    }

    protected abstract fun getNewId(path: String): String

    abstract fun update(values: Map<String, Any?>, callback: DatabaseCallback)

    fun newRootUserCustomTimeId(userKey: UserKey) = getNewId("$USERS_KEY/${userKey.key}/${CustomTimeRecord.CUSTOM_TIMES}")

    fun newSharedProjectRecordId() = ProjectKey.Shared(getNewId(RECORDS_KEY))

    // root tasks

    fun newRootTaskRecordId() = getNewId(TASKS_KEY)

    fun newRootTaskScheduleRecordId(taskId: String) =
        ScheduleId(getNewId("$TASKS_KEY/$taskId/${ScheduleRecord.SCHEDULES}"))

    fun newRootTaskNoScheduleOrParentRecordId(taskId: String) =
        getNewId("$TASKS_KEY/$taskId/${NoScheduleOrParentRecord.NO_SCHEDULE_OR_PARENT}")

    fun newRootTaskNestedTaskHierarchyRecordId(taskId: String) =
        TaskHierarchyId(getNewId("$TASKS_KEY/$taskId/${TaskHierarchyRecord.TASK_HIERARCHIES}"))

    fun newProjectOrdinalEntryId(userKey: UserKey, projectKey: ProjectKey.Shared) =
        getNewId("$USERS_KEY/${userKey.key}/${RootUserRecord.ORDINAL_ENTRIES}/${projectKey.key}")

    abstract fun checkTrackers(
        userKeys: Set<UserKey>,
        privateProjectKeys: Set<ProjectKey.Private>,
        sharedProjectKeys: Set<ProjectKey.Shared>,
        taskKeys: Set<TaskKey.Root>,
    )
}