package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.checkme.firebase.loaders.DatabaseRx
import com.krystianwsul.checkme.firebase.loaders.MapChanges
import com.krystianwsul.checkme.firebase.loaders.processChanges
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksLoader(
        rootTaskKeySource: RootTaskKeySource,
        private val provider: Provider,
        private val domainDisposable: CompositeDisposable,
        val rootTasksManager: AndroidRootTasksManager,
        private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
) {

    private val taskKeyRelay = ReplayRelay.create<Map<TaskKey.Root, RootTaskRecord?>>()

    private var ignoreKeyUpdates = false

    init {
        rootTaskKeySource.rootTaskKeysObservable
                .filter { !ignoreKeyUpdates }
                .map { it.associateWith<TaskKey.Root, RootTaskRecord?> { null } }
                .subscribe(taskKeyRelay)
                .addTo(domainDisposable)
    }

    private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

    private data class TaskData(val databaseRx: DatabaseRx<Snapshot<RootTaskJson>>, var initialRecord: RootTaskRecord?)

    private val databaseRxObservable: Observable<MapChanges<Map<TaskKey.Root, RootTaskRecord?>, TaskKey.Root, TaskData>> = taskKeyRelay.processChanges(
            { it.keys },
            { map, taskKey ->
                val initialRecord = map.getValue(taskKey)

                TaskData(
                        DatabaseRx(
                                CompositeDisposable(),
                                provider.getRootTaskObservable(taskKey),
                        ),
                        initialRecord,
                )
            },
            { it.databaseRx.disposable.dispose() },
    ).replayImmediate()

    private data class RecordData(val record: RootTaskRecord, val isAddedLocally: Boolean)

    val addChangeEvents: Observable<AddChangeEvent> = databaseRxObservable.switchMap { mapChanges ->
        mapChanges.newMap
                .map { (_, taskData) ->
                    val recordObservable = taskData.databaseRx
                            .observable
                            .mapNotNull(rootTasksManager::set)
                            .map { RecordData(it, false) }
                            .let {
                                val initialRecord = taskData.initialRecord
                                if (initialRecord != null) {
                                    taskData.initialRecord = null

                                    it.startWithItem(RecordData(initialRecord, true))
                                } else {
                                    it
                                }
                            }

                    recordObservable.map { (taskRecord, isAddedLocally) ->
                        val isTaskKeyTracked = loadDependencyTrackerManager.isTaskKeyTracked(taskRecord.taskKey)

                        // there's no reason why we'd be tracking a change for a locally added record
                        check(!isTaskKeyTracked || !isAddedLocally)

                        AddChangeEvent(taskRecord, isTaskKeyTracked || isAddedLocally)
                    }
                }.merge()
    }.replayImmediate()

    val removeEvents: Observable<RemoveEvent> = databaseRxObservable.map { it.removedEntries }
            .filter { it.isNotEmpty() }
            .map { RemoveEvent(it.keys) }
            .replayImmediate()

    fun addTask(taskJson: RootTaskJson): TaskKey.Root {
        val taskRecord = rootTasksManager.newTaskRecord(taskJson)
        val taskKey = taskRecord.taskKey

        val currentKeyMap = taskKeyRelay.value!!
        check(!currentKeyMap.containsKey(taskKey))

        taskKeyRelay.accept(
                currentKeyMap.toMutableMap().apply { put(taskRecord.taskKey, taskRecord) }
        )

        return taskKey
    }

    fun ignoreKeyUpdates(action: () -> Unit) {
        check(!ignoreKeyUpdates)

        ignoreKeyUpdates = true
        action()
        check(ignoreKeyUpdates)

        ignoreKeyUpdates = false
    }

    data class AddChangeEvent(val rootTaskRecord: RootTaskRecord, val skipChangeTypeEmission: Boolean)

    data class RemoveEvent(val taskKeys: Set<TaskKey.Root>)

    interface Provider {

        fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>>
    }
}