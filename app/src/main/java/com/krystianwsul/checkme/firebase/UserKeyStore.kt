package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class UserKeyStore(
        friendKeysObservable: Observable<ChangeWrapper<Set<UserKey>>>,
        domainDisposable: CompositeDisposable,
) {

    private val loadUserDataRelay = ReplayRelay.create<ChangeWrapper<Map<UserKey, LoadUserData>>>()

    val loadUserDataObservable = loadUserDataRelay.hide()

    init {
        friendKeysObservable.map {
            it.newData<Map<UserKey, LoadUserData>>(it.data.associateWith { LoadUserData.Friend(null) })
        }
                .subscribe(loadUserDataRelay)
                .addTo(domainDisposable)
    }

    fun addFriend(rootUserRecord: RootUserRecord) {
        val addFriendDatas = loadUserDataRelay.value // todo source account for new friend already in custom time users
                .data
                .toMutableMap()

        check(!addFriendDatas.containsKey(rootUserRecord.userKey))

        addFriendDatas[rootUserRecord.userKey] = rootUserRecord.run { LoadUserData.Friend(AddFriendData(key, userWrapper)) }

        loadUserDataRelay.accept(ChangeWrapper(ChangeType.LOCAL, addFriendDatas))
    }

    fun requestCustomTimeUsers(userKeys: Set<UserKey>) {
        // todo source
    }

    sealed class LoadUserData {

        abstract val userLoadReason: UserLoadReason
        abstract val addFriendData: AddFriendData?

        data class Friend(override val addFriendData: AddFriendData?) : LoadUserData() {

            override val userLoadReason = UserLoadReason.FRIEND
        }

        object CustomTimes : LoadUserData() {

            override val userLoadReason = UserLoadReason.CUSTOM_TIMES

            override val addFriendData: AddFriendData? = null
        }
    }

    data class AddFriendData(val key: String, val userWrapper: UserWrapper)
}