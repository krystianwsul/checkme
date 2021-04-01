package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.loaders.snapshot.UntypedSnapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class FriendsLoader(
        friendKeysObservable: Observable<ChangeWrapper<Set<UserKey>>>,
        private val domainDisposable: CompositeDisposable,
        private val friendsProvider: FriendsProvider
) {

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private val databaseRx = friendKeysObservable.processChanges(
            { it.data },
            { _, userKey ->
                DatabaseRx(
                        domainDisposable,
                        friendsProvider.database.getUserObservable(userKey)
                )
            },
            { it.disposable.dispose() }
    ).replayImmediate()

    val initialFriendsEvent = databaseRx.firstOrError()
            .flatMap {
                it.newMap
                        .values
                        .map { it.first }
                        .zipSingle()
            }
            .map(::InitialFriendsEvent)
            .cacheImmediate(domainDisposable)

    private val addFriendEvents = databaseRx.skip(1)
            .switchMap {
                it.addedEntries
                        .values
                        .map { it.first.toObservable() }
                        .merge()
            }
            .map(::AddChangeFriendEvent)

    private val changeFriendEvents = databaseRx.switchMap {
        it.newMap
                .values
                .map { it.changes }
                .merge()
    }.map(::AddChangeFriendEvent)

    val addChangeFriendEvents = listOf(addFriendEvents, changeFriendEvents).merge().replayImmediate()

    val removeFriendEvents = databaseRx.map { RemoveFriendsEvent(it.original.changeType, it.removedEntries.keys) }
            .filter { it.userKeys.isNotEmpty() }
            .replayImmediate()

    class InitialFriendsEvent(val snapshots: Iterable<Snapshot>)

    class AddChangeFriendEvent(val snapshot: UntypedSnapshot)

    class RemoveFriendsEvent(val userChangeType: ChangeType, val userKeys: Set<UserKey>)
}