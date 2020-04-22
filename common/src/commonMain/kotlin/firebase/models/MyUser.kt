package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


class MyUser(private val remoteMyUserRecord: MyUserRecord) :
        RootUser(remoteMyUserRecord),
        MyUserProperties by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    var projectChangeListener: (() -> Unit)? = null // because I can't add a relay to common
    var friendChangeListener: (() -> Unit)? = null

    override fun addProject(projectKey: ProjectKey.Shared) {
        super.addProject(projectKey)

        projectChangeListener?.invoke()
    }

    override fun removeProject(projectKey: ProjectKey.Shared): Boolean {
        val result = super.removeProject(projectKey)

        projectChangeListener?.invoke()

        return result
    }

    override fun addFriend(userKey: UserKey) {
        super.addFriend(userKey)

        friendChangeListener?.invoke()
    }

    override fun removeFriend(userKey: UserKey) {
        super.removeFriend(userKey)

        friendChangeListener?.invoke()
    }
}
