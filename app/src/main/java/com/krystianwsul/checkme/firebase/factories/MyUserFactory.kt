package com.krystianwsul.checkme.firebase.factories

import com.badoo.reaktive.rxjavainterop.asRxJava3Observable
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.MyUserManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.models.MyUser
import io.reactivex.rxjava3.core.Observable

class MyUserFactory(
        userSnapshot: Snapshot,
        deviceDbInfo: DeviceDbInfo,
        private val factoryProvider: FactoryProvider
) {

    private val myUserManager = MyUserManager(deviceDbInfo, userSnapshot)

    private val userRelay = BehaviorRelay.createDefault(MyUser(myUserManager.value))

    var user
        get() = userRelay.value!!
        private set(value) = userRelay.accept(value)

    val isSaved get() = myUserManager.isSaved

    val sharedProjectKeysObservable = Observable.merge(
            userRelay.map { ChangeType.REMOTE },
            userRelay.switchMap { it.projectChanges.asRxJava3Observable() }.map { ChangeType.LOCAL }
    )
            .map { ChangeWrapper(it, user.projectIds) }
            .distinctUntilChanged()!!

    val friendKeysObservable = userRelay.switchMap { myUser ->
        myUser.friendChanges
                .asRxJava3Observable()
                .map { ChangeType.LOCAL }
                .startWithItem(ChangeType.REMOTE)
                .map { ChangeWrapper(it, myUser.friends) }
    }.distinctUntilChanged()!!

    val savedList get() = myUserManager.savedList

    init {
        setTab()

        user.name = deviceDbInfo.name
        user.setToken(deviceDbInfo)
    }

    private fun setTab() {
        factoryProvider.preferences.tab = user.defaultTab
    }

    fun onNewSnapshot(snapshot: Snapshot): ChangeType {
        val changeWrapper = myUserManager.set(snapshot)
        user = MyUser(changeWrapper.data)
        setTab()

        return changeWrapper.changeType
    }

    fun save(values: MutableMap<String, Any?>) = myUserManager.save(values)
}
