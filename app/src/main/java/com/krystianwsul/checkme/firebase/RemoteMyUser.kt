package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemoteMyUserRecord


class RemoteMyUser(private val remoteMyUserRecord: RemoteMyUserRecord) : RemoteRootUser(remoteMyUserRecord) {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    fun setToken(token: String?) = remoteMyUserRecord.setToken(token)
}
