package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.records.RemoteUserManager

class RemoteUserFactory(
        domainFactory: DomainFactory,
        userSnapshot: DataSnapshot,
        userInfo: UserInfo) {

    private val remoteUserManager = RemoteUserManager(domainFactory, userInfo, domainFactory.uuid, userSnapshot)

    var remoteUser = RemoteMyUser(remoteUserManager.remoteUserRecord)
        private set

    var isSaved
        get() = remoteUserManager.isSaved
        set(value) {
            remoteUserManager.isSaved = value
        }

    fun onNewSnapshot(dataSnapshot: DataSnapshot) {
        val remoteUserRecord = remoteUserManager.newSnapshot(dataSnapshot)

        remoteUser = RemoteMyUser(remoteUserRecord)
    }

    fun save() = remoteUserManager.save()
}
