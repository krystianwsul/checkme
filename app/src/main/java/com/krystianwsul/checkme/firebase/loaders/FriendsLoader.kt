package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class FriendsLoader(
        val userKeyStore: UserKeyStore,
        private val domainDisposable: CompositeDisposable,
        private val friendsProvider: FriendsProvider,
) {

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private val databaseRx: Observable<MapChanges<ChangeWrapper<Map<UserKey, UserKeyStore.LoadUserData>>, UserKey, DatabaseRx<Snapshot<UserWrapper>>>> =
            userKeyStore.loadUserDataObservable
                    .processChanges(
                            { it.data.keys },
                            { (_, userDatas), userKey ->
                                val addFriendData = userDatas.getValue(userKey).addFriendData

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
                    )
                    .replayImmediate()

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

    class InitialFriendsEvent(val userWrapperDatas: List<UserWrapperData>)

    class AddChangeFriendEvent(val userWrapperData: UserWrapperData)

    class RemoveFriendsEvent(val userChangeType: ChangeType, val userKeys: Set<UserKey>)

    data class UserWrapperData(val reason: UserLoadReason, val snapshot: Snapshot<UserWrapper>)
}