package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.firebase.records.RemoteMyUserRecord
import com.krystianwsul.common.firebase.RemoteMyUserInterface


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
