package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.loaders.DatabaseRx
import com.krystianwsul.checkme.firebase.loaders.MapChanges
import com.krystianwsul.checkme.firebase.loaders.processChanges
import com.krystianwsul.checkme.firebase.managers.RootTaskManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTaskLoader(
        taskKeysObservable: Observable<Map<TaskKey.Root, ProjectKey<*>>>,
        private val provider: Provider,
        private val domainDisposable: CompositeDisposable,
        private val rootTaskManager: RootTaskManager,
) {

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private val databaseRxObservable: Observable<MapChanges<Map<TaskKey.Root, ProjectKey<*>>, TaskKey.Root, DatabaseRx<Snapshot<RootTaskJson>>>> = taskKeysObservable.processChanges(
            { it.keys },
            { _, taskKey ->
                DatabaseRx(
                        CompositeDisposable(),
                        provider.getRootTaskObservable(taskKey),
                )
            },
            { it.disposable.dispose() },
    ).replayImmediate()

    val addChangeEvents: Observable<AddChangeEvent> = databaseRxObservable.switchMap { mapChanges ->
        mapChanges.newMap
                .map { (taskKey, databaseRx) ->
                    databaseRx.observable
                            .mapNotNull { rootTaskManager.set(it) }
                            .map { AddChangeEvent(it, mapChanges.original.getValue(taskKey)) }
                }.merge()
    }.replayImmediate()

    val removeEvents: Observable<RemoveEvent> = databaseRxObservable.map { it.removedEntries }
            .filter { it.isNotEmpty() }
            .map { RemoveEvent(it.keys) }
            .replayImmediate()

    data class AddChangeEvent(val rootTaskRecord: RootTaskRecord, val projectKey: ProjectKey<*>)

    data class RemoveEvent(val taskKeys: Set<TaskKey.Root>)

    interface Provider {

        fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>>
    }
}