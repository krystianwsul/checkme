package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteSharedProjectRecord(
        private val databaseWrapper: DatabaseWrapper,
        private val parent: Parent,
        create: Boolean,
        id: ProjectKey.Shared,
        private val jsonWrapper: JsonWrapper
) : RemoteProjectRecord<RemoteCustomTimeId.Shared, SharedProjectJson, ProjectKey.Shared>(
        create,
        id,
        jsonWrapper.projectJson
) {

    override val remoteCustomTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val remoteCustomTimeId = RemoteCustomTimeId.Shared(id)

                remoteCustomTimeId to RemoteSharedCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    val remoteUserRecords by lazy {
        projectJson.users
                .entries
                .associate { (id, userJson) ->
                    check(id.isNotEmpty())

                    ProjectKey.Private(id) to RemoteProjectUserRecord(create, this, userJson)
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

    fun updateRecordOf(addedFriends: Set<ProjectKey.Private>, removedFriends: Set<ProjectKey.Private>) {
        check(addedFriends.none { removedFriends.contains(it) })

        jsonWrapper.updateRecordOf(addedFriends, removedFriends)

        for (addedFriend in addedFriends)
            addValue("$id/recordOf/$addedFriend", true)

        for (removedFriend in removedFriends)
            addValue("$id/recordOf/$removedFriend", null)
    }

    fun newRemoteCustomTimeRecord(customTimeJson: SharedCustomTimeJson): RemoteSharedCustomTimeRecord {
        val remoteCustomTimeRecord = RemoteSharedCustomTimeRecord(this, customTimeJson)
        check(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.id))

        remoteCustomTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    fun newRemoteUserRecord(userJson: UserJson): RemoteProjectUserRecord {
        val remoteProjectUserRecord = RemoteProjectUserRecord(true, this, userJson)
        check(!remoteUserRecords.containsKey(remoteProjectUserRecord.id))

        remoteUserRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
        return remoteProjectUserRecord
    }

    private val createProjectJson
        get() = projectJson.apply {
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id.value }, { it.createObject })
                    .toMutableMap()

            users = remoteUserRecords.values
                    .associateBy({ it.id.key }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = "$key/$PROJECT_JSON"

    override fun deleteFromParent() = parent.deleteRemoteSharedProjectRecord(id)

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Shared(databaseWrapper.newSharedCustomTimeRecordId(id))

    override fun getTaskRecordId() = databaseWrapper.newSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = databaseWrapper.newSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = databaseWrapper.newSharedTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Shared(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    override fun getRemoteCustomTimeKey(remoteCustomTimeId: RemoteCustomTimeId.Shared) = CustomTimeKey.Shared(id, remoteCustomTimeId)

    interface Parent {

        fun deleteRemoteSharedProjectRecord(id: ProjectKey.Shared)
    }
}