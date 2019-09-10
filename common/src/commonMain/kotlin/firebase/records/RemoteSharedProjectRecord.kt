package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteSharedProjectRecord(
        private val parent: Parent,
        create: Boolean,
        id: String,
        uuid: String,
        private val jsonWrapper: JsonWrapper
) : RemoteProjectRecord<RemoteCustomTimeId.Shared>(
        create,
        id,
        uuid
) {

    override val projectJson = jsonWrapper.projectJson

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
                .mapValues { (id, userJson) ->
                    check(id.isNotEmpty())

                    RemoteProjectUserRecord(create, this, userJson)
                }
                .toMutableMap()
    }

    override val children get() = super.children + remoteUserRecords.values

    constructor(
            parent: Parent,
            uuid: String,
            id: String,
            jsonWrapper: JsonWrapper
    ) : this(
            parent,
            false,
            id,
            uuid,
            jsonWrapper
    )

    constructor(
            parent: Parent,
            uuid: String,
            jsonWrapper: JsonWrapper
    ) : this(
            parent,
            true,
            DatabaseWrapper.instance.newSharedProjectRecordId(),
            uuid,
            jsonWrapper
    )

    fun updateRecordOf(addedFriends: Set<String>, removedFriends: Set<String>) {
        check(addedFriends.none { removedFriends.contains(it) })

        jsonWrapper.updateRecordOf(addedFriends, removedFriends)

        for (addedFriend in addedFriends) {
            addValue("$id/recordOf/$addedFriend", true)
        }

        for (removedFriend in removedFriends) {
            addValue("$id/recordOf/$removedFriend", null)
        }
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
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = "$key/$PROJECT_JSON"

    override fun deleteFromParent() = parent.deleteRemoteSharedProjectRecord(id)

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Shared(DatabaseWrapper.instance.newSharedCustomTimeRecordId(id))

    override fun getTaskRecordId() = DatabaseWrapper.instance.newSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = DatabaseWrapper.instance.newSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = DatabaseWrapper.instance.newSharedTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Shared(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    override fun getRemoteCustomTimeKey(projectId: String, customTimeId: String) = CustomTimeKey.Shared(projectId, getRemoteCustomTimeId(customTimeId))

    interface Parent {

        fun deleteRemoteSharedProjectRecord(id: String)
    }
}