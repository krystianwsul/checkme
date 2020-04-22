package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.checkme.firebase.managers.RemoteMyUserManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.MyUser

class RemoteUserFactory(
        userSnapshot: Snapshot,
        deviceDbInfo: DeviceDbInfo,
        private val factoryProvider: FactoryProvider
) {

    private val remoteUserManager = RemoteMyUserManager(deviceDbInfo, userSnapshot)

    private val remoteUserRelay = BehaviorRelay.createDefault(MyUser(remoteUserManager.remoteUserRecord))

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

    private val projectIdTrigger = BehaviorRelay.createDefault(ChangeType.REMOTE)
    private val friendKeyTrigger = BehaviorRelay.createDefault(Unit)

    init {
        setTab()

        remoteUser.setToken(deviceDbInfo)

        remoteUser.projectChangeListener = {
            projectIdTrigger.accept(ChangeType.LOCAL)
        }
        remoteUser.friendChangeListener = { friendKeyTrigger.accept(Unit) }
    }

    val sharedProjectKeysObservable = projectIdTrigger.map {
        ChangeWrapper(it, remoteUser.projectIds)
    }.distinctUntilChanged()!!

    val friendKeysObservable = friendKeyTrigger.switchMap {
        remoteUserRelay.map { it.friends }
    }.distinctUntilChanged()!!

    private fun setTab() {
        factoryProvider.preferences.tab = remoteUser.defaultTab
    }

    fun onNewSnapshot(dataSnapshot: Snapshot) {
        remoteUser = MyUser(remoteUserManager.newSnapshot(dataSnapshot))
        setTab()

        projectIdTrigger.accept(ChangeType.REMOTE)
    }

    fun save(values: MutableMap<String, Any?>) = remoteUserManager.save(values)
}
