package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.customtime.PrivateCustomTime
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class PrivateProject(
    override val projectRecord: PrivateOwnedProjectRecord,
    userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    rootTaskProvider: RootTaskProvider,
    rootModelChangeManager: RootModelChangeManager,
) : OwnedProject<ProjectType.Private>(
    AssignedToHelper.Private,
    userCustomTimeProvider,
    rootTaskProvider,
    rootModelChangeManager,
) {

    override val projectKey = projectRecord.projectKey

    override val remoteCustomTimes = HashMap<CustomTimeId.Project.Private, PrivateCustomTime>()
    override val _tasks: MutableMap<String, ProjectTask>
    override val taskHierarchyContainer = TaskHierarchyContainer()

    override val customTimes get() = remoteCustomTimes.values

    var defaultTimesCreated
        get() = projectRecord.defaultTimesCreated
        set(value) {
            projectRecord.defaultTimesCreated = value
        }

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        _tasks = projectRecord.taskRecords
            .values
            .map { ProjectTask(this, it) }
            .associateBy { it.id }
            .toMutableMap()

        projectRecord.taskHierarchyRecords
            .values
            .map { ProjectTaskHierarchy(this, it) }
            .forEach { taskHierarchyContainer.add(it.id, it) }
    }

    override fun deleteCustomTime(remoteCustomTime: Time.Custom.Project<ProjectType.Private>) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project) =
        remoteCustomTimes.getValue(projectCustomTimeId as CustomTimeId.Project.Private)

    private fun tryGetProjectCustomTime(projectCustomTimeId: CustomTimeId.Project) =
        remoteCustomTimes[projectCustomTimeId as CustomTimeId.Project.Private]

    override fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Private>) =
        tryGetProjectCustomTime(projectCustomTimeKey)!!

    fun tryGetProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Private>) =
        tryGetProjectCustomTime(projectCustomTimeKey.customTimeId)

    override fun getAssignedTo(userKeys: Set<UserKey>) = mapOf<UserKey, ProjectUser>()
}