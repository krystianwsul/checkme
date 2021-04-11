package com.krystianwsul.common.firebase.records

import com.badoo.reaktive.subject.behavior.BehaviorSubject
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.SharedTaskJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.*

class SharedProjectRecord(
        private val databaseWrapper: DatabaseWrapper,
        private val parent: Parent,
        create: Boolean,
        override val projectKey: ProjectKey.Shared,
        private val jsonWrapper: JsonWrapper
) : ProjectRecord<ProjectType.Shared>(
        create,
        jsonWrapper.projectJson,
        projectKey,
        "${projectKey.key}/$PROJECT_JSON"
) {

    override val taskRecordsRelay = BehaviorSubject(
            projectJson.tasks
                    .mapValues { (id, taskJson) ->
                        check(id.isNotEmpty())

                        SharedTaskRecord(id, this, taskJson)
                    }
    )

    override val taskRecords: Map<String, SharedTaskRecord> get() = taskRecordsRelay.value

    fun mutateTaskRecords(action: (MutableMap<String, SharedTaskRecord>) -> Unit) {
        taskRecordsRelay.onNext(taskRecords.toMutableMap().also(action))
    }

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

    constructor(
            databaseWrapper: DatabaseWrapper,
            parent: Parent,
            id: ProjectKey.Shared,
            jsonWrapper: JsonWrapper
    ) : this(
            databaseWrapper,
            parent,
            false,
            id,
            jsonWrapper
    )

    constructor(
            databaseWrapper: DatabaseWrapper,
            parent: Parent,
            jsonWrapper: JsonWrapper
    ) : this(
            databaseWrapper,
            parent,
            true,
            databaseWrapper.newSharedProjectRecordId(),
            jsonWrapper
    )

    fun newRemoteCustomTimeRecord(customTimeJson: SharedCustomTimeJson): SharedCustomTimeRecord {
        val remoteCustomTimeRecord = SharedCustomTimeRecord(this, customTimeJson)
        check(!customTimeRecords.containsKey(remoteCustomTimeRecord.id))

        customTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

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
                    .associateBy({ it.id }, { it.createObject })
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

    fun getCustomTimeRecordId() =
            CustomTimeId.Project.Shared(databaseWrapper.newSharedCustomTimeRecordId(projectKey))

    override fun getTaskRecordId() = databaseWrapper.newSharedTaskRecordId(projectKey)

    override fun getScheduleRecordId(taskId: String) =
            databaseWrapper.newSharedScheduleRecordId(projectKey, taskId)

    override fun getTaskHierarchyRecordId() =
            databaseWrapper.newSharedTaskHierarchyRecordId(projectKey)

    override fun getCustomTimeRecord(id: String) =
            customTimeRecords.getValue(CustomTimeId.Project.Shared(id))

    override fun getProjectCustomTimeId(id: String) = CustomTimeId.Project.Shared(id)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project<ProjectType.Shared>) =
            CustomTimeKey.Project.Shared(projectKey, projectCustomTimeId as CustomTimeId.Project.Shared)

    override fun newNoScheduleOrParentRecordId(taskId: String) =
            databaseWrapper.newSharedNoScheduleOrParentRecordId(projectKey, taskId)

    fun newTaskRecord(taskJson: SharedTaskJson): SharedTaskRecord {
        val remoteTaskRecord = SharedTaskRecord(this, taskJson)
        check(!taskRecords.containsKey(remoteTaskRecord.id))

        mutateTaskRecords {
            it[remoteTaskRecord.id] = remoteTaskRecord
        }

        return remoteTaskRecord
    }

    interface Parent {

        fun remove(key: ProjectKey.Shared)
    }
}