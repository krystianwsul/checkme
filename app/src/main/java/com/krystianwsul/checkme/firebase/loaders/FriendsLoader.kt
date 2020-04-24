package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge

class FriendsLoader(
        friendKeysObservable: Observable<Set<UserKey>>,
        private val domainDisposable: CompositeDisposable,
        private val friendsProvider: FriendsProvider
) {

    private val databaseRx = friendKeysObservable.processChangesSet(
            {
                DatabaseRx(
                        domainDisposable,
                        friendsProvider.database.getUserObservable(it)
                )
            },
            { it.disposable.dispose() }
    )

    val initialFriendsEvent = databaseRx.firstOrError()
            .flatMap {
                it.newMap
                        .map { it.value.first }
                        .zipSingle()
            }
            .map(::InitialFriendsEvent)

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

    val addChangeFriendEvents = listOf(addFriendEvents, changeFriendEvents).merge()

    val removeFriendEvents = databaseRx.map {
        RemoveFriendsEvent(it.removedEntries.keys) // todo friends this might need the changeType propagated
    }

    class InitialFriendsEvent(val snapshots: Iterable<Snapshot>)

    class AddChangeFriendEvent(val snapshot: Snapshot)

    class RemoveFriendsEvent(val userKeys: Set<UserKey>)
}