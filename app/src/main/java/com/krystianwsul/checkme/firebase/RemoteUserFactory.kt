package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.managers.RemoteUserManager
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.models.RemoteMyUser

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

    init {
        setTab()
    }

    private fun setTab() {
        Preferences.tab = remoteUser.defaultTab
    }

    fun onNewSnapshot(dataSnapshot: DataSnapshot) {
        val remoteUserRecord = remoteUserManager.newSnapshot(dataSnapshot)

        remoteUser = RemoteMyUser(remoteUserRecord)

        setTab()
    }

    fun save() = remoteUserManager.save()
}
