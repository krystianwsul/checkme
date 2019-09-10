package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteSharedProjectRecord(
        private val remoteSharedProjectManager: RemoteSharedProjectManager,
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
            remoteSharedProjectManager: RemoteSharedProjectManager,
            uuid: String,
            id: String,
            jsonWrapper: JsonWrapper
    ) : this(
            remoteSharedProjectManager,
            false,
            id,
            uuid,
            jsonWrapper
    )

    constructor(
            remoteSharedProjectManager: RemoteSharedProjectManager,
            uuid: String,
            jsonWrapper: JsonWrapper
    ) : this(
            remoteSharedProjectManager,
            true,
            AndroidDatabaseWrapper.newSharedProjectRecordId(),
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

    override fun deleteFromParent() = check(remoteSharedProjectManager.remoteProjectRecords.remove(id) == this)

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Shared(AndroidDatabaseWrapper.newSharedCustomTimeRecordId(id))

    override fun getTaskRecordId() = AndroidDatabaseWrapper.newSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = AndroidDatabaseWrapper.newSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = AndroidDatabaseWrapper.newSharedTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Shared(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    override fun getRemoteCustomTimeKey(projectId: String, customTimeId: String) = CustomTimeKey.Shared(projectId, getRemoteCustomTimeId(customTimeId))
}