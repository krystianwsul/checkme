package com.krystianwsul.checkme.firebase.factories

import com.badoo.reaktive.rxjavainterop.asRxJava3Observable
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.managers.MyUserManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.MyUser

class MyUserFactory(userSnapshot: Snapshot<UserWrapper>, deviceDbInfo: DeviceDbInfo, databaseWrapper: DatabaseWrapper) {

    private val myUserManager = MyUserManager(deviceDbInfo, userSnapshot, databaseWrapper)

    private val userRelay = BehaviorRelay.createDefault(MyUser(myUserManager.value))

    var user
        get() = userRelay.value!!
        private set(value) = userRelay.accept(value)

    val sharedProjectKeysObservable = userRelay.map { it.projectIds }.distinctUntilChanged()!!

    val friendKeysObservable = userRelay.switchMap { myUser ->
        myUser.friendChanges
                .asRxJava3Observable()
                .map { ChangeType.LOCAL }
                .startWithItem(ChangeType.REMOTE)
                .map { ChangeWrapper(it, myUser.friends) }
    }.distinctUntilChanged()!!

    init {
        user.name = deviceDbInfo.name
        user.setToken(deviceDbInfo)
    }

    fun onNewSnapshot(snapshot: Snapshot<UserWrapper>): ChangeType? {
        return myUserManager.set(snapshot)?.let {
            user = MyUser(it.data)

            it.changeType
        }
    }

    fun save(values: MutableMap<String, Any?>) = myUserManager.save(values)
}
