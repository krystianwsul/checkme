package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.customtimes.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.records.customtime.PrivateCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.PrivateTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class PrivateProjectRecord(
        private val databaseWrapper: DatabaseWrapper,
        create: Boolean,
        override val projectKey: ProjectKey.Private,
        private val projectJson: PrivateProjectJson,
) : ProjectRecord<ProjectType.Private>(
        create,
        projectJson,
        projectKey,
        projectKey.key,
) {

    override val taskRecords = projectJson.tasks
            .mapValues { (id, taskJson) ->
                check(id.isNotEmpty())

                PrivateTaskRecord(id, this, taskJson)
            }
            .toMutableMap()

    override val customTimeRecords: MutableMap<CustomTimeId.Project.Private, PrivateCustomTimeRecord>

    init {
        initTaskHierarchyRecords()

        customTimeRecords = projectJson.customTimes
                .entries
                .associate { (id, customTimeJson) ->
                    check(id.isNotEmpty())

                    val customTimeId = CustomTimeId.Project.Private(id)

                    customTimeId to PrivateCustomTimeRecord(customTimeId, this, customTimeJson)
                }
                .toMutableMap()
    }

    constructor(
            databaseWrapper: DatabaseWrapper,
            id: ProjectKey.Private,
            projectJson: PrivateProjectJson
    ) : this(databaseWrapper, false, id, projectJson)

    constructor(
            databaseWrapper: DatabaseWrapper,
            userInfo: UserInfo,
            projectJson: PrivateProjectJson
    ) : this(databaseWrapper, true, userInfo.key.toPrivateProjectKey(), projectJson)

    fun newRemoteCustomTimeRecord(customTimeJson: PrivateCustomTimeJson): PrivateCustomTimeRecord {
        val remoteCustomTimeRecord = PrivateCustomTimeRecord(this, customTimeJson)
        check(!customTimeRecords.containsKey(remoteCustomTimeRecord.id))

        customTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    private val createProjectJson
        get() = projectJson.apply {
            tasks = taskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = taskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = customTimeRecords.values
                    .associateBy({ it.id.value }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    var defaultTimesCreated by Committer(projectJson::defaultTimesCreated, "$key/$PROJECT_JSON")

    override fun deleteFromParent() = throw UnsupportedOperationException()

    fun getCustomTimeRecordId() =
            CustomTimeId.Project.Private(databaseWrapper.newPrivateCustomTimeRecordId(projectKey))

    override fun getTaskRecordId() = databaseWrapper.newPrivateTaskRecordId(projectKey)

    override fun getScheduleRecordId(taskId: String) =
            databaseWrapper.newPrivateScheduleRecordId(projectKey, taskId)

    override fun getProjectTaskHierarchyRecordId() =
            databaseWrapper.newPrivateProjectTaskHierarchyRecordId(projectKey)

    override fun newNestedTaskHierarchyRecordId(taskId: String) =
            databaseWrapper.newPrivateNestedTaskHierarchyRecordId(projectKey, taskId)

    override fun getCustomTimeRecord(id: String) =
            customTimeRecords.getValue(CustomTimeId.Project.Private(id))

    override fun getProjectCustomTimeId(id: String) = CustomTimeId.Project.Private(id)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project) =
            CustomTimeKey.Project.Private(projectKey, projectCustomTimeId as CustomTimeId.Project.Private)

    override fun newNoScheduleOrParentRecordId(taskId: String) =
            databaseWrapper.newPrivateNoScheduleOrParentRecordId(projectKey, taskId)

    fun newTaskRecord(taskJson: PrivateTaskJson): PrivateTaskRecord {
        val remoteTaskRecord = PrivateTaskRecord(this, taskJson)
        check(!taskRecords.containsKey(remoteTaskRecord.id))

        taskRecords[remoteTaskRecord.id] = remoteTaskRecord

        return remoteTaskRecord
    }

    override fun deleteTaskRecord(taskRecord: TaskRecord) {
        check(taskRecords.remove(taskRecord.id) == taskRecord)
    }
}