package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson

class RemoteSharedProjectRecord(
        private val remoteSharedProjectManager: RemoteSharedProjectManager,
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        private val jsonWrapper: JsonWrapper) : RemoteProjectRecord(create, domainFactory, id) {

    override val projectJson = jsonWrapper.projectJson

    override val remoteCustomTimeRecords = projectJson.customTimes
            .mapValues { (id, customTimeJson) ->
                check(!TextUtils.isEmpty(id))

                RemoteSharedCustomTimeRecord(id, this, customTimeJson)
            }
            .toMutableMap()

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

    private val createProjectJson
        get() = projectJson.apply {
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            users = remoteUserRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = key + "/" + RemoteProjectRecord.PROJECT_JSON

    override fun deleteFromParent() = check(remoteSharedProjectManager.remoteProjectRecords.remove(id) == this)

    override fun getCustomTimeRecordId() = DatabaseWrapper.getSharedCustomTimeRecordId(id)

    override fun getTaskRecordId() = DatabaseWrapper.getSharedTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = DatabaseWrapper.getSharedScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = DatabaseWrapper.getSharedTaskHierarchyRecordId(id)
}