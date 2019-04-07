package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemoteMyUserRecord


class RemoteMyUser(private val remoteMyUserRecord: RemoteMyUserRecord) : RemoteRootUser(remoteMyUserRecord) {

    fun setToken(token: String?) = remoteMyUserRecord.setToken(token)
}
