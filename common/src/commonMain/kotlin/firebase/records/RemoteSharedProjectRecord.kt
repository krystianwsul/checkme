package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.*

class RemoteSharedProjectRecord(
        private val databaseWrapper: DatabaseWrapper,
        private val parent: Parent,
        create: Boolean,
        override val id: ProjectKey.Shared,
        private val jsonWrapper: JsonWrapper
) : RemoteProjectRecord<ProjectType.Shared>(
        create,
        jsonWrapper.projectJson,
        id
) {

    override val customTimeRecords = jsonWrapper.projectJson
            .customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val customTimeId = CustomTimeId.Shared(id)

                customTimeId to SharedCustomTimeRecord(customTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    val remoteUserRecords by lazy {
        jsonWrapper.projectJson
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
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
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

    override fun deleteFromParent() = parent.deleteRemoteSharedProjectRecord(id)

    fun getCustomTimeRecordId() = CustomTimeId.Shared(databaseWrapper.newSharedCustomTimeRecordId(id))

    override fun getTaskRecordId() = databaseWrapper.newSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = databaseWrapper.newSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = databaseWrapper.newSharedTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = customTimeRecords.getValue(CustomTimeId.Shared(id))

    override fun getCustomTimeId(id: String) = CustomTimeId.Shared(id)

    override fun getRemoteCustomTimeKey(customTimeId: CustomTimeId<ProjectType.Shared>) = CustomTimeKey.Shared(id, customTimeId)

    interface Parent {

        fun deleteRemoteSharedProjectRecord(id: ProjectKey<ProjectType.Shared>)
    }
}