package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.RemoteMyUserInterface
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.ProjectKey


class MyUser(private val remoteMyUserRecord: MyUserRecord) : RootUser(remoteMyUserRecord), RemoteMyUserInterface by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    var projectChangeListener: (() -> Unit)? = null // because I can't add a relay to common

    override fun addProject(projectKey: ProjectKey.Shared) {
        super.addProject(projectKey)

        projectChangeListener?.invoke()
    }

    override fun removeProject(projectKey: ProjectKey.Shared): Boolean {
        val result = super.removeProject(projectKey)

        projectChangeListener?.invoke()

        return result
    }
}
