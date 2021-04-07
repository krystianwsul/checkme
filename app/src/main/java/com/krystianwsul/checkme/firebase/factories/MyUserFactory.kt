package com.krystianwsul.checkme.firebase.factories

import com.badoo.reaktive.rxjavainterop.asRxJava3Observable
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.managers.MyUserManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.MyUser

class MyUserFactory(userSnapshot: Snapshot<UserWrapper>, deviceDbInfo: DeviceDbInfo) {

    private val myUserManager = MyUserManager(deviceDbInfo, userSnapshot)

    private val userRelay = BehaviorRelay.createDefault(MyUser(myUserManager.value))

    var user
        get() = userRelay.value!!
        private set(value) = userRelay.accept(value)

    val isSaved get() = myUserManager.isSaved

    val sharedProjectKeysObservable =
            userRelay.map { ChangeWrapper(ChangeType.REMOTE, it.projectIds) }.distinctUntilChanged()!!

    val friendKeysObservable = userRelay.switchMap { myUser ->
        myUser.friendChanges
                .asRxJava3Observable()
                .map { ChangeType.LOCAL }
                .startWithItem(ChangeType.REMOTE)
                .map { ChangeWrapper(it, myUser.friends) }
    }.distinctUntilChanged()!!

    val savedList get() = myUserManager.savedList

    init {
        user.name = deviceDbInfo.name
        user.setToken(deviceDbInfo)
    }

    fun onNewSnapshot(snapshot: Snapshot<UserWrapper>): ChangeType {
        val changeWrapper = myUserManager.set(snapshot)
        user = MyUser(changeWrapper.data)

        return changeWrapper.changeType
    }

    fun save(values: MutableMap<String, Any?>) = myUserManager.save(values)
}
