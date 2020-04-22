package com.krystianwsul.checkme.firebase

import com.badoo.reaktive.rxjavainterop.asRxJava2Observable
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.checkme.firebase.managers.RemoteMyUserManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.MyUser
import io.reactivex.Observable

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

    init {
        setTab()

        remoteUser.setToken(deviceDbInfo)
    }

    val sharedProjectKeysObservable = Observable.merge(
            remoteUserRelay.map { ChangeType.REMOTE },
            remoteUserRelay.switchMap { it.projectChanges.asRxJava2Observable() }.map { ChangeType.LOCAL }
    )
            .map { ChangeWrapper(it, remoteUser.projectIds) }
            .distinctUntilChanged()!!

    val friendKeysObservable = remoteUserRelay.switchMap { myUser ->
        myUser.friendChanges
                .asRxJava2Observable()
                .startWith(Unit)
                .map { myUser.friends }
    }.distinctUntilChanged()!!

    private fun setTab() {
        factoryProvider.preferences.tab = remoteUser.defaultTab
    }

    fun onNewSnapshot(dataSnapshot: Snapshot) {
        remoteUser = MyUser(remoteUserManager.newSnapshot(dataSnapshot))
        setTab()
    }

    fun save(values: MutableMap<String, Any?>) = remoteUserManager.save(values)
}
