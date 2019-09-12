package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteRootUserRecord


open class RemoteRootUser(private val remoteRootUserRecord: RemoteRootUserRecord) {

    val id by lazy { remoteRootUserRecord.id }

    val name get() = remoteRootUserRecord.name

    val email get() = remoteRootUserRecord.email

    val userJson get() = remoteRootUserRecord.userJson

    open val photoUrl get() = remoteRootUserRecord.photoUrl

    fun removeFriend(friendId: String) {
        check(friendId.isNotEmpty())

        remoteRootUserRecord.removeFriendOf(friendId)
    }
}
