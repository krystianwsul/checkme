package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.managers.MyUserManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.MyUser
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import kotlinx.coroutines.rx3.asObservable

class MyUserFactory(
    userSnapshot: Snapshot<UserWrapper>,
    deviceDbInfo: DeviceDbInfo,
    databaseWrapper: DatabaseWrapper,
    private val rootModelChangeManager: RootModelChangeManager,
) {

    private val myUserManager = MyUserManager(deviceDbInfo, userSnapshot, databaseWrapper)

    private val userRelay = BehaviorRelay.createDefault(MyUser(myUserManager.value, rootModelChangeManager))

    init {
        rootModelChangeManager.invalidateUsers()
    }

    var user
        get() = userRelay.value!!
        private set(value) = userRelay.accept(value)

    val sharedProjectKeysObservable = userRelay.map { it.projectIds }.distinctUntilChanged()

    val friendKeysObservable = userRelay.switchMap { myUser ->
        myUser.friendChanges
            .asObservable()
            .doOnNext { DomainThreadChecker.instance.requireDomainThread() }
            .map { ChangeType.LOCAL }
            .startWithItem(ChangeType.REMOTE)
            .map { ChangeWrapper(it, myUser.friends) }
    }

    init {
        user.name = deviceDbInfo.name
        user.setToken(deviceDbInfo)
    }

    fun onNewSnapshot(snapshot: Snapshot<UserWrapper>): ChangeType? {
        return myUserManager.set(snapshot)?.let {
            user.clearableInvalidatableManager.clear()
            rootModelChangeManager.invalidateUsers()

            user = MyUser(it.data, rootModelChangeManager)

            it.changeType
        }
    }

    fun save(values: MutableMap<String, Any?>) = myUserManager.save(values)
}
