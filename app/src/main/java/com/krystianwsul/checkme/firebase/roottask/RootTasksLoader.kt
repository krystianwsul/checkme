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
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksLoader(
    rootTaskKeySource: RootTaskKeySource,
    private val provider: Provider,
    private val domainDisposable: CompositeDisposable,
    val rootTasksManager: AndroidRootTasksManager,
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

    private fun <T : Any> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }

    private data class TaskData(val databaseRx: DatabaseRx<Snapshot<RootTaskJson>>, var initialRecord: RootTaskRecord?)

    private val databaseRxObservable: Observable<MapChanges<Map<TaskKey.Root, RootTaskRecord?>, TaskKey.Root, TaskData>> =
        taskKeyRelay.processChanges(
            { it.keys },
            { map, taskKey ->
                val initialRecord = map.getValue(taskKey)

                TaskData(
                    DatabaseRx(domainDisposable, provider.getRootTaskObservable(taskKey)),
                    initialRecord,
                )
            },
            { it.databaseRx.disposable.dispose() },
        ).replayImmediate()

    private data class RecordData(val record: RootTaskRecord?, val isAddedLocally: Boolean)

    private val databaseEvents: Observable<Event> = databaseRxObservable.switchMap { mapChanges ->
        mapChanges.newMap
            .map { (taskKey, taskData) ->
                val recordObservable = taskData.databaseRx
                    .observable
                    .mapNotNull(rootTasksManager::set)
                    .map { RecordData(it.value, false) }
                    .let {
                        val initialRecord = taskData.initialRecord
                        if (initialRecord != null) {
                            taskData.initialRecord = null

                            var first = true

                            it.filter {
                                /**
                                 * This is awful, but I don't feel like building it more robustly for now.  When we add a
                                 * task, the initial firebase event may return null.  But we don't want to skip other null
                                 * events, since they'd indicate that the task was indeed removed from the DB.
                                 */
                                val skip = first && it.record == null

                                first = false

                                !skip
                            }.startWithItem(RecordData(initialRecord, true))
                        } else {
                            it
                        }
                    }

                recordObservable.map { (taskRecord, isAddedLocally) ->
                    taskRecord?.let { AddChangeEvent(it, isAddedLocally) } ?: RemoveEvent(setOf(taskKey))
                }
            }.merge()
    }.replayImmediate()

    private val removeEntryEvents: Observable<RemoveEvent> = databaseRxObservable.map { it.removedEntries }
        .filter { it.isNotEmpty() }
        .map { RemoveEvent(it.keys) }
        .replayImmediate()

    private val allEvents = listOf(databaseEvents, removeEntryEvents).merge()

    val addChangeEvents = allEvents.ofType<AddChangeEvent>()

    val removeEvents: Observable<RemoveEvent> = allEvents.ofType<RemoveEvent>()
        .doOnNext { it.taskKeys.forEach(rootTasksManager::remove) }
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

    sealed interface Event

    data class AddChangeEvent(val rootTaskRecord: RootTaskRecord, val skipChangeTypeEmission: Boolean) : Event

    data class RemoveEvent(val taskKeys: Set<TaskKey.Root>) : Event

    interface Provider {

        fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>>
    }
}