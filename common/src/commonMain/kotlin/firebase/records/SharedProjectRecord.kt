package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
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
        projectKey
) {

    override lateinit var customTimeRecords: MutableMap<CustomTimeId.Shared, SharedCustomTimeRecord>
        private set

    lateinit var remoteUserRecords: MutableMap<UserKey, RemoteProjectUserRecord>
        private set

    init {
        initChildRecords(create)
    }

    override fun initChildRecords(create: Boolean) {
        super.initChildRecords(create)

        customTimeRecords = jsonWrapper.projectJson
                .customTimes
                .map { (id, customTimeJson) ->
                    check(id.isNotEmpty())

                    val customTimeId = CustomTimeId.Shared(id)

                    customTimeId to SharedCustomTimeRecord(customTimeId, this, customTimeJson)
                }
                .toMap()
                .toMutableMap()

        remoteUserRecords = jsonWrapper.projectJson
                .users
                .entries
                .associate { (id, userJson) ->
                    check(id.isNotEmpty())

                    UserKey(id) to RemoteProjectUserRecord(create, this, userJson)
                }
                .toMutableMap()
    }

    override val children get() = super.children + remoteUserRecords.values

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

    fun newRemoteUserRecord(userJson: UserJson): RemoteProjectUserRecord {
        val remoteProjectUserRecord = RemoteProjectUserRecord(true, this, userJson)
        check(!remoteUserRecords.containsKey(remoteProjectUserRecord.id))

        remoteUserRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
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

            users = remoteUserRecords.values
                    .associateBy({ it.id.key }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = "$key/$PROJECT_JSON"

    override fun deleteFromParent() = parent.deleteRemoteSharedProjectRecord(projectKey)

    fun getCustomTimeRecordId() = CustomTimeId.Shared(databaseWrapper.newSharedCustomTimeRecordId(projectKey))

    override fun getTaskRecordId() = databaseWrapper.newSharedTaskRecordId(projectKey)

    override fun getScheduleRecordId(taskId: String) = databaseWrapper.newSharedScheduleRecordId(projectKey, taskId)

    override fun getTaskHierarchyRecordId() = databaseWrapper.newSharedTaskHierarchyRecordId(projectKey)

    override fun getCustomTimeRecord(id: String) = customTimeRecords.getValue(CustomTimeId.Shared(id))

    override fun getCustomTimeId(id: String) = CustomTimeId.Shared(id)

    override fun getCustomTimeKey(customTimeId: CustomTimeId<ProjectType.Shared>) = CustomTimeKey.Shared(projectKey, customTimeId)

    interface Parent {

        fun deleteRemoteSharedProjectRecord(projectKey: ProjectKey.Shared)
    }
}