package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class FriendsLoader(
        friendKeysObservable: Observable<ChangeWrapper<Set<UserKey>>>,
        private val domainDisposable: CompositeDisposable,
        private val friendsProvider: FriendsProvider,
) {

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private sealed class LoadUserData {

        abstract val userLoadReason: UserLoadReason

        data class Friend(val addFriendData: AddFriendData?) : LoadUserData() {

            override val userLoadReason = UserLoadReason.FRIEND
        }

        object CustomTimes : LoadUserData() {

            override val userLoadReason = UserLoadReason.CUSTOM_TIMES
        }
    }

    private data class AddFriendData(val key: String, val userWrapper: UserWrapper)

    private val loadUserDataRelay = ReplayRelay.create<ChangeWrapper<Map<UserKey, LoadUserData>>>()

    init {
        friendKeysObservable.map {
            it.newData<Map<UserKey, LoadUserData>>(it.data.associateWith { LoadUserData.Friend(null) })
        }
                .subscribe(loadUserDataRelay)
                .addTo(domainDisposable)
    }

    private val databaseRx: Observable<MapChanges<ChangeWrapper<Map<UserKey, LoadUserData>>, UserKey, DatabaseRx<Snapshot<UserWrapper>>>> =
            loadUserDataRelay.processChanges(
                    { it.data.keys },
                    { (_, userDatas), userKey ->
                        val addFriendData = (userDatas.getValue(userKey) as? LoadUserData.Friend)?.addFriendData

                        DatabaseRx(
                                domainDisposable,
                                friendsProvider.database
                                        .getUserObservable(userKey)
                                        .let {
                                            if (addFriendData != null) {
                                                it.startWithItem(Snapshot(addFriendData.key, addFriendData.userWrapper))
                                            } else {
                                                it
                                            }
                                        },
                        )
                    },
                    { it.disposable.dispose() },
            ).replayImmediate()

    val initialFriendsEvent: Single<InitialFriendsEvent> = databaseRx.firstOrError()
            .doOnSuccess { check(it.original.changeType == ChangeType.REMOTE) }
            .flatMap {
                val loadUserDataMap = it.original.data

                it.newMap
                        .map { (userKey, databaseRx) ->
                            val reason = loadUserDataMap.getValue(userKey).userLoadReason

                            databaseRx.first.map { UserWrapperData(reason, it) }
                        }
                        .zipSingle()
            }
            .map(::InitialFriendsEvent)
            .cacheImmediate(domainDisposable)

    private val addFriendEvents: Observable<AddChangeFriendEvent> = databaseRx.skip(1)
            .switchMap {
                val loadUserDataMap = it.original.data

                it.addedEntries
                        .map { (userKey, databaseRx) ->
                            val reason = loadUserDataMap.getValue(userKey).userLoadReason

                            databaseRx.first
                                    .toObservable()
                                    .map { UserWrapperData(reason, it) }
                        }
                        .merge()
            }
            .map(::AddChangeFriendEvent)

    private val changeFriendEvents: Observable<AddChangeFriendEvent> = databaseRx.switchMap {
        val loadUserDataMap = it.original.data

        it.newMap
                .map { (userKey, databaseRx) ->
                    val reason = loadUserDataMap.getValue(userKey).userLoadReason

                    databaseRx.changes.map { UserWrapperData(reason, it) }
                }
                .merge()
    }.map(::AddChangeFriendEvent)

    val addChangeFriendEvents: Observable<AddChangeFriendEvent> =
            listOf(addFriendEvents, changeFriendEvents).merge().replayImmediate()

    val removeFriendEvents = databaseRx.map { RemoveFriendsEvent(it.original.changeType, it.removedEntries.keys) }
            .filter { it.userKeys.isNotEmpty() }
            .replayImmediate()

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

    class InitialFriendsEvent(val userWrapperDatas: List<UserWrapperData>)

    class AddChangeFriendEvent(val userWrapperData: UserWrapperData)

    class RemoveFriendsEvent(val userChangeType: ChangeType, val userKeys: Set<UserKey>)

    data class UserWrapperData(val reason: UserLoadReason, val snapshot: Snapshot<UserWrapper>)
}