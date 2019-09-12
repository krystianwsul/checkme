package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.RemoteMyUserInterface
import com.krystianwsul.common.firebase.records.RemoteMyUserRecord


class RemoteMyUser(private val remoteMyUserRecord: RemoteMyUserRecord) : RemoteRootUser(remoteMyUserRecord), RemoteMyUserInterface by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }
}
