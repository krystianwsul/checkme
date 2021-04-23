package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.loaders.DatabaseRx
import com.krystianwsul.checkme.firebase.loaders.MapChanges
import com.krystianwsul.checkme.firebase.loaders.processChanges
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTaskLoader(
        private val taskKeysObservable: Observable<Set<TaskKey.Root>>,
        private val provider: Provider,
        private val domainDisposable: CompositeDisposable,
) {

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private val databaseRxObservable: Observable<MapChanges<Set<TaskKey.Root>, TaskKey.Root, DatabaseRx<Snapshot<RootTaskJson>>>> = taskKeysObservable.processChanges(
            { it },
            { _, taskKey ->
                DatabaseRx(
                        CompositeDisposable(),
                        provider.getRootTaskObservable(taskKey),
                )
            },
            { it.disposable.dispose() },
    ).replayImmediate()

    private val addChangeEvents: Observable<AddChangeEvent> = databaseRxObservable.switchMap { mapChanges ->
        mapChanges.newMap
                .map { (taskKey, databaseRx) ->
                    databaseRx.observable.map { AddChangeEvent(taskKey, it) }
                }.merge()
    }.replayImmediate()

    private val removeEvents: Observable<RemoveEvent> = databaseRxObservable.map { it.removedEntries }
            .filter { it.isNotEmpty() }
            .map { RemoveEvent(it.keys) }
            .replayImmediate()

    private data class AddChangeEvent(val taskKey: TaskKey.Root, val snapshot: Snapshot<RootTaskJson>)

    private data class RemoveEvent(val taskKeys: Set<TaskKey.Root>)

    interface Provider {

        fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>>
    }
}