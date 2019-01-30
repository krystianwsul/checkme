package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper

class RemoteSharedProjectRecord(
        private val remoteSharedProjectManager: RemoteSharedProjectManager,
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        private val jsonWrapper: JsonWrapper) : RemoteProjectRecord(create, domainFactory, id, jsonWrapper.projectJson) {

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

    override val createObject get() = jsonWrapper.apply { projectJson = createProjectJson }

    override val childKey get() = key + "/" + RemoteProjectRecord.PROJECT_JSON

    override fun deleteFromParent() = check(remoteSharedProjectManager.remoteProjectRecords.remove(id) == this)
}