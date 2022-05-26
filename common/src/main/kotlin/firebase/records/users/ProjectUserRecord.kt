package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord


abstract class ProjectUserRecord(
    create: Boolean,
    private val remoteProjectRecord: SharedOwnedProjectRecord, // todo projectKey
    final override val createObject: UserJson
) : RemoteRecord(create) {

    companion object {

        const val USERS = "users"
    }

    val id by lazy { UserData.getKey(createObject.email) }

    final override val key by lazy { remoteProjectRecord.childKey + "/" + USERS + "/" + id.key }

    abstract val name: String

    val email by lazy { createObject.email }

    abstract val photoUrl: String?
}
