package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.managers.RemoteUserManager
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.models.RemoteMyUser

class RemoteUserFactory(
        uuid: String,
        userSnapshot: DataSnapshot,
        deviceInfo: DeviceInfo
) {

    private val remoteUserManager = RemoteUserManager(deviceInfo, uuid, userSnapshot)

    private val remoteUserRelay = BehaviorRelay.createDefault(RemoteMyUser(remoteUserManager.remoteUserRecord))

    var remoteUser
        get() = remoteUserRelay.value!!
        set(value) {
            remoteUserRelay.accept(value)
        }

    var isSaved
        get() = remoteUserManager.isSaved
        set(value) {
            remoteUserManager.isSaved = value
        }

    private val projectIdTrigger = BehaviorRelay.createDefault(Unit)

    init {
        setTab()

        remoteUser.setToken(uuid, deviceInfo.token)

        remoteUser.projectChangeListener = { projectIdTrigger.accept(Unit) }
    }

    val sharedProjectKeysObservable = projectIdTrigger.switchMap {
        remoteUserRelay.map { it.projectIds }
    }.distinctUntilChanged()!!

    private fun setTab() {
        Preferences.tab = remoteUser.defaultTab
    }

    fun onNewSnapshot(dataSnapshot: DataSnapshot) {
        val remoteUserRecord = remoteUserManager.newSnapshot(dataSnapshot)

        remoteUser = RemoteMyUser(remoteUserRecord)

        setTab()
    }

    fun save(domainFactory: DomainFactory) = remoteUserManager.save(domainFactory)
}
