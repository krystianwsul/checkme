package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.ofType

class RecursiveTaskRecordLoader(
        taskRecord: RootTaskRecord,
        taskRecordLoader: TaskRecordLoader,
        rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
) {

    val completable: Completable

    init {
        val taskKey = taskRecord.taskKey
        val loadingRecord = TaskLoadState.LoadingRecord(taskKey, taskRecordLoader, rootTaskUserCustomTimeProviderSource)
        val fakeInitialMap = mapOf(taskKey to loadingRecord)

        val initialMap = RecordLoadedMutator(
                taskRecord,
                taskRecordLoader,
                rootTaskUserCustomTimeProviderSource,
        ).mutateMap(fakeInitialMap)

        val initialState = TreeLoadState(initialMap)

        val stateRelay = BehaviorRelay.createDefault(initialState)

        val getRecordsState = stateRelay.switchMapSingle {
            it.getNextState()
                    .map<GetRecordsState> { GetRecordsState.Loading(it) }
                    .defaultIfEmpty(GetRecordsState.Loaded)
        }.share()

        val stateProcessor =
                getRecordsState.ofType<GetRecordsState.Loading>().doOnNext { stateRelay.accept(it.treeLoadState) }

        val loadedObservable = getRecordsState.ofType<GetRecordsState.Loaded>()

        completable = listOf(
                stateProcessor.ignoreElements().toObservable(), // never completes, just hiding subscription
                loadedObservable,
        ).merge()
                .firstOrError()
                .ignoreElement()
    }

    sealed class GetRecordsState {

        class Loading(val treeLoadState: TreeLoadState) : GetRecordsState()

        object Loaded : GetRecordsState()
    }

    class TreeLoadState(private val taskLoadStates: Map<TaskKey.Root, TaskLoadState>) {

        fun getNextState(): Maybe<TreeLoadState> {
            val mutatorSingles = taskLoadStates.mapNotNull { it.value.mutator }

            return if (mutatorSingles.isEmpty()) {
                Maybe.empty()
            } else {
                val nextMutatorSingle = mutatorSingles.map { it.toObservable() }
                        .merge()
                        .firstOrError()

                nextMutatorSingle.map { TreeLoadState(it.mutateMap(taskLoadStates)) }.toMaybe()
            }
        }
    }

    sealed class TaskLoadState {

        abstract val mutator: Single<TaskLoadStateMapMutator>?

        class LoadingRecord(
                val taskKey: TaskKey.Root,
                private val taskRecordLoader: TaskRecordLoader,
                private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        ) : TaskLoadState() {

            override val mutator = taskRecordLoader.getTaskRecordSingle(taskKey)
                    .map<TaskLoadStateMapMutator> {
                        RecordLoadedMutator(it, taskRecordLoader, rootTaskUserCustomTimeProviderSource)
                    }
                    .cache()!!
        }

        class LoadingTimes(
                private val taskRecord: RootTaskRecord,
                rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        ) : TaskLoadState() {

            override val mutator = rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord)
                    .map<TaskLoadStateMapMutator> { TimesLoadedMutator(taskRecord) }
                    .cache()!!
        }

        object Loaded : TaskLoadState() {

            override val mutator: Single<TaskLoadStateMapMutator>? = null
        }
    }

    interface TaskLoadStateMapMutator {

        fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState>
    }

    class RecordLoadedMutator(
            private val taskRecord: RootTaskRecord,
            private val taskRecordLoader: TaskRecordLoader,
            private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
    ) : TaskLoadStateMapMutator {

        override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
            val parentKeys = taskRecord.taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }
            val childKeys = taskRecord.rootTaskParentDelegate.rootTaskKeys
            val allKeys = (parentKeys + childKeys).toSet()

            val newKeys = allKeys - oldMap.keys

            val newEntries = newKeys.map {
                it to TaskLoadState.LoadingRecord(it, taskRecordLoader, rootTaskUserCustomTimeProviderSource)
            }

            val newMap = (oldMap + newEntries).toMutableMap()
            check(newMap.getValue(taskRecord.taskKey) is TaskLoadState.LoadingRecord)

            newMap[taskRecord.taskKey] =
                    TaskLoadState.LoadingTimes(taskRecord, rootTaskUserCustomTimeProviderSource)

            return newMap
        }
    }

    class TimesLoadedMutator(private val taskRecord: RootTaskRecord) : TaskLoadStateMapMutator {

        override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
            check(oldMap.containsKey(taskRecord.taskKey))

            return oldMap.toMutableMap().also { it[taskRecord.taskKey] = TaskLoadState.Loaded }
        }
    }

    interface TaskRecordLoader {

        fun getTaskRecordSingle(taskKey: TaskKey.Root): Single<RootTaskRecord>

        class Impl(private val rootTasksLoader: RootTasksLoader, domainDisposable: CompositeDisposable) :
                TaskRecordLoader {

            private val currentlyLoadedKeys = mutableMapOf<TaskKey.Root, RootTaskRecord>()

            init {
                rootTasksLoader.addChangeEvents
                        .subscribe { currentlyLoadedKeys[it.rootTaskRecord.taskKey] = it.rootTaskRecord }
                        .addTo(domainDisposable)

                rootTasksLoader.removeEvents
                        .subscribe { currentlyLoadedKeys -= it.taskKeys }
                        .addTo(domainDisposable)
            }

            override fun getTaskRecordSingle(taskKey: TaskKey.Root): Single<RootTaskRecord> {
                currentlyLoadedKeys[taskKey]?.let { return Single.just(it) }

                return rootTasksLoader.addChangeEvents
                        .filter { it.rootTaskRecord.taskKey == taskKey }
                        .firstOrError()
                        .map { it.rootTaskRecord }
            }
        }
    }
}