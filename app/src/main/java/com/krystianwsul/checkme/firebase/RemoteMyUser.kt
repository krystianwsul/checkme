package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemoteMyUserRecord


class RemoteMyUser(private val remoteMyUserRecord: RemoteMyUserRecord) : RemoteRootUser(remoteMyUserRecord), RemoteMyUserInterface by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }
}
