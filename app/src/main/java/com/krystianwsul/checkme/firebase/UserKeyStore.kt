package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class UserKeyStore(
        friendKeysObservable: Observable<ChangeWrapper<Set<UserKey>>>,
        domainDisposable: CompositeDisposable,
) {

    private val addFriendEvents = PublishRelay.create<FriendEvent.AddFriend>()

    val loadUserDataObservable: Observable<ChangeWrapper<Map<UserKey, LoadUserData>>>

    init {
        val friendKeysChangeEvents = friendKeysObservable.map { FriendEvent.FriendKeysChange(it) }

        loadUserDataObservable = listOf(friendKeysChangeEvents, addFriendEvents).merge()
                .scan(
                        ChangeWrapper<Map<UserKey, LoadUserData>>(ChangeType.LOCAL, mapOf()) // this will be ignored by skip
                ) { oldChangeWrapper, friendEvent ->
                    when (friendEvent) {
                        is FriendEvent.FriendKeysChange -> { // overwrite
                            val changeWrapper = friendEvent.changeWrapper

                            changeWrapper.newData(
                                    changeWrapper.data.associateWith { LoadUserData.Friend(null) }
                            )
                        }
                        is FriendEvent.AddFriend -> { // add to map
                            val newMap = oldChangeWrapper.data.toMutableMap()
                            newMap[friendEvent.rootUserRecord.userKey] = LoadUserData.Friend(AddFriendData(
                                    friendEvent.rootUserRecord.key,
                                    friendEvent.rootUserRecord.userWrapper
                            ))

                            ChangeWrapper(ChangeType.LOCAL, newMap)
                        }
                    }
                }
                .skip(1)
                .replay()
                .apply { domainDisposable += connect() }
    }

    fun addFriend(rootUserRecord: RootUserRecord) {
        // todo source account for new friend already in custom time users
        addFriendEvents.accept(FriendEvent.AddFriend(rootUserRecord))
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

    private sealed class FriendEvent {

        data class FriendKeysChange(val changeWrapper: ChangeWrapper<Set<UserKey>>) : FriendEvent()

        data class AddFriend(val rootUserRecord: RootUserRecord) : FriendEvent()
    }
}