package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.records.ProjectUserRecord
import com.krystianwsul.common.firebase.records.customtime.SharedCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.SharedTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.*

class SharedProjectRecord(
    private val parent: Parent,
    create: Boolean,
    override val projectKey: ProjectKey.Shared,
    private val jsonWrapper: JsonWrapper,
) : ProjectRecord<ProjectType.Shared>(
    create,
    jsonWrapper.projectJson,
    projectKey,
    "${projectKey.key}/$PROJECT_JSON"
) {

    override val taskRecords = projectJson.tasks
        .mapValues { (id, taskJson) ->
            check(id.isNotEmpty())

            SharedTaskRecord(id, this, taskJson)
        }
        .toMutableMap()

    override val customTimeRecords: MutableMap<CustomTimeId.Project.Shared, SharedCustomTimeRecord>

    var userRecords: MutableMap<UserKey, ProjectUserRecord>
        private set

    private val projectJson get() = jsonWrapper.projectJson

    init {
        initTaskHierarchyRecords()

        customTimeRecords = jsonWrapper.projectJson
            .customTimes
            .entries
            .associate { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val customTimeId = CustomTimeId.Project.Shared(id)

                customTimeId to SharedCustomTimeRecord(customTimeId, this, customTimeJson)
            }
            .toMutableMap()

        userRecords = jsonWrapper.projectJson
            .users
            .entries
            .associate { (id, userJson) ->
                check(id.isNotEmpty())

                UserKey(id) to ProjectUserRecord(create, this, userJson)
            }
            .toMutableMap()
    }

    override val children get() = super.children + userRecords.values

    constructor(parent: Parent, id: ProjectKey.Shared, jsonWrapper: JsonWrapper) : this(parent, false, id, jsonWrapper)

    constructor(databaseWrapper: DatabaseWrapper, parent: Parent, jsonWrapper: JsonWrapper) :
            this(parent, true, databaseWrapper.newSharedProjectRecordId(), jsonWrapper)

    fun newRemoteUserRecord(userJson: UserJson): ProjectUserRecord {
        val remoteProjectUserRecord = ProjectUserRecord(true, this, userJson)
        check(!userRecords.containsKey(remoteProjectUserRecord.id))

        userRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
        return remoteProjectUserRecord
    }

    private val createProjectJson
        get() = jsonWrapper.projectJson.apply {
            tasks = taskRecords.values
                .associateBy({ it.id }, { it.createObject })
                .toMutableMap()

            taskHierarchies = taskHierarchyRecords.values
                .associateBy({ it.id.value }, { it.createObject })
                .toMutableMap()

            customTimes = customTimeRecords.values
                .associateBy({ it.id.value }, { it.createObject })
                .toMutableMap()

            users = userRecords.values
                .associateBy({ it.id.key }, { it.createObject })
                .toMutableMap()
        }

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = "$key/$PROJECT_JSON"

    override fun deleteFromParent() = parent.remove(projectKey)

    override fun getCustomTimeRecord(id: String) =
        customTimeRecords.getValue(CustomTimeId.Project.Shared(id))

    override fun getProjectCustomTimeId(id: String) = CustomTimeId.Project.Shared(id)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project) =
        CustomTimeKey.Project.Shared(projectKey, projectCustomTimeId as CustomTimeId.Project.Shared)

    override fun deleteTaskRecord(taskRecord: TaskRecord) {
        check(taskRecords.remove(taskRecord.id) == taskRecord)
    }

    interface Parent {

        fun remove(key: ProjectKey.Shared)
    }
}