package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DeviceInfo
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.managers.RemoteUserManager

class RemoteUserFactory(
        domainFactory: DomainFactory,
        userSnapshot: DataSnapshot,
        deviceInfo: DeviceInfo) {

    private val remoteUserManager = RemoteUserManager(domainFactory, deviceInfo, domainFactory.uuid, userSnapshot)

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
