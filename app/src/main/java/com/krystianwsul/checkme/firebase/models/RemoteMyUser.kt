package com.krystianwsul.checkme.firebase.models

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.common.firebase.RemoteMyUserInterface
import com.krystianwsul.common.firebase.records.RemoteMyUserRecord


class RemoteMyUser(private val remoteMyUserRecord: RemoteMyUserRecord) : RemoteRootUser(remoteMyUserRecord), RemoteMyUserInterface by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    init {
        Preferences.tab = defaultTab
    }
}
