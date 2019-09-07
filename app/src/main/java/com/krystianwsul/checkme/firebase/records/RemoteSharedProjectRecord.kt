package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.common.firebase.JsonWrapper
import com.krystianwsul.common.firebase.SharedCustomTimeJson
import com.krystianwsul.common.firebase.UserJson

class RemoteSharedProjectRecord(
        private val remoteSharedProjectManager: RemoteSharedProjectManager,
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        private val jsonWrapper: JsonWrapper) : RemoteProjectRecord<RemoteCustomTimeId.Shared>(create, domainFactory, id) {

    override val projectJson = jsonWrapper.projectJson

    override val remoteCustomTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(!TextUtils.isEmpty(id))

                val remoteCustomTimeId = RemoteCustomTimeId.Shared(id)

                remoteCustomTimeId to RemoteSharedCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    val remoteUserRecords by lazy {
        projectJson.users
                .mapValues { (id, userJson) ->
                    check(!TextUtils.isEmpty(id))

                    RemoteProjectUserRecord(create, this, userJson)
                }
                .toMutableMap()
    }

    override val children get() = super.children + remoteUserRecords.values

    constructor(remoteSharedProjectManager: RemoteSharedProjectManager, domainFactory: DomainFactory, id: String, jsonWrapper: JsonWrapper) : this(
            remoteSharedProjectManager,
            false,
            domainFactory,
            id,
            jsonWrapper)

    constructor(remoteSharedProjectManager: RemoteSharedProjectManager, domainFactory: DomainFactory, jsonWrapper: JsonWrapper) : this(
            remoteSharedProjectManager,
            true,
            domainFactory,
            DatabaseWrapper.getRootRecordId(),
            jsonWrapper)

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

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Shared(DatabaseWrapper.getSharedCustomTimeRecordId(id))

    override fun getTaskRecordId() = DatabaseWrapper.getSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = DatabaseWrapper.getSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = DatabaseWrapper.getSharedTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Shared(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Shared(id)

    override fun getRemoteCustomTimeKey(projectId: String, customTimeId: String) = CustomTimeKey.Shared(projectId, getRemoteCustomTimeId(customTimeId))
}