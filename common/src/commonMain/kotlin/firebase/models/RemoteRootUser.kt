package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteRootUserRecord


open class RemoteRootUser(private val remoteRootUserRecord: RemoteRootUserRecord) { // todo delegate

    val id by lazy { remoteRootUserRecord.id }

    val name get() = remoteRootUserRecord.name

    val email get() = remoteRootUserRecord.email

    val userJson get() = remoteRootUserRecord.userJson

    open val photoUrl get() = remoteRootUserRecord.photoUrl

    fun removeFriend(friendId: String) = remoteRootUserRecord.removeFriendOf(friendId)

    fun addProject(projectId: String) = remoteRootUserRecord.addProject(projectId)

    fun removeProject(projectId: String) = remoteRootUserRecord.removeProject(projectId)
}
