package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.krystianwsul.checkme.firebase.records.RemoteRootUserRecord


open class RemoteRootUser(private val remoteRootUserRecord: RemoteRootUserRecord) {

    val id by lazy { remoteRootUserRecord.id }

    val name get() = remoteRootUserRecord.name

    val email get() = remoteRootUserRecord.email

    val userJson get() = remoteRootUserRecord.userJson

    fun removeFriend(friendId: String) {
        check(!TextUtils.isEmpty(friendId))

        remoteRootUserRecord.removeFriendOf(friendId)
    }
}
