package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.loaders.DatabaseRx
import com.krystianwsul.checkme.firebase.loaders.MapChanges
import com.krystianwsul.checkme.firebase.loaders.processChanges
import com.krystianwsul.checkme.firebase.managers.RootTasksManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksLoader(
        taskKeysObservable: Observable<Set<TaskKey.Root>>,
        private val provider: Provider,
        private val domainDisposable: CompositeDisposable,
        private val rootTasksManager: RootTasksManager,
        private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
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

    val addChangeEvents: Observable<AddChangeEvent> = databaseRxObservable.switchMap { mapChanges ->
        mapChanges.newMap
                .map { (_, databaseRx) ->
                    databaseRx.observable
                            .mapNotNull(rootTasksManager::set)
                            .map { AddChangeEvent(it, loadDependencyTrackerManager.isTaskKeyTracked(it.taskKey)) }
                }.merge()
    }.replayImmediate()

    val removeEvents: Observable<RemoveEvent> = databaseRxObservable.map { it.removedEntries }
            .filter { it.isNotEmpty() }
            .map { RemoveEvent(it.keys) }
            .replayImmediate()

    fun addTask(taskJson: RootTaskJson): RootTaskRecord {
        val taskRecord = rootTasksManager.newTaskRecord(taskJson)

        // todo task create start add to RX

        return taskRecord
    }

    data class AddChangeEvent(val rootTaskRecord: RootTaskRecord, val isTracked: Boolean)

    data class RemoveEvent(val taskKeys: Set<TaskKey.Root>)

    interface Provider {

        fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>>
    }
}