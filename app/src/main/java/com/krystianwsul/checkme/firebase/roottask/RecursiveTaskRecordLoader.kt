package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.utils.replayImmediate
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
        userCustomTimeProviderSource: UserCustomTimeProviderSource,
        domainDisposable: CompositeDisposable,
) {

    val completable: Completable

    init {
        val taskKey = taskRecord.taskKey
        val loadingRecord = TaskLoadState.LoadingRecord(taskKey, taskRecordLoader, userCustomTimeProviderSource)
        val fakeInitialMap = mapOf(taskKey to loadingRecord)

        val initialMap = RecordLoadedMutator(
                taskRecord,
                taskRecordLoader,
                userCustomTimeProviderSource,
        ).mutateMap(fakeInitialMap)

        val initialState = TreeLoadState(initialMap)

        val stateRelay = BehaviorRelay.createDefault(initialState)

        val getRecordsState = stateRelay.switchMapSingle {
            it.getNextState()
                    .map<GetRecordsState> { GetRecordsState.Loading(it) }
                    .defaultIfEmpty(GetRecordsState.Loaded)
        }.replayImmediate(domainDisposable) // just for tests

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

    class TreeLoadState(val taskLoadStates: Map<TaskKey.Root, TaskLoadState>) {

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
                private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
        ) : TaskLoadState() {

            override val mutator = taskRecordLoader.getTaskRecordSingle(taskKey)
                    .map<TaskLoadStateMapMutator> {
                        RecordLoadedMutator(it, taskRecordLoader, userCustomTimeProviderSource)
                    }
                    .cache()!!

            override fun toString() = "LoadingRecord $taskKey"
        }

        class LoadingTimes(
                private val taskRecord: RootTaskRecord,
                userCustomTimeProviderSource: UserCustomTimeProviderSource,
        ) : TaskLoadState() {

            override val mutator = userCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord)
                    .map<TaskLoadStateMapMutator> { TimesLoadedMutator(taskRecord) }
                    .cache()!!

            override fun toString() = "LoadingTimes ${taskRecord.taskKey}"
        }

        class Loaded(private val taskKey: TaskKey.Root) : TaskLoadState() {

            override val mutator: Single<TaskLoadStateMapMutator>? = null

            override fun toString() = "Loaded $taskKey"
        }
    }

    interface TaskLoadStateMapMutator {

        fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState>
    }

    class RecordLoadedMutator(
            private val taskRecord: RootTaskRecord,
            private val taskRecordLoader: TaskRecordLoader,
            private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
    ) : TaskLoadStateMapMutator {

        override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
            val newKeys = taskRecord.getDependentTaskKeys() - oldMap.keys

            val newEntries = newKeys.map {
                it to TaskLoadState.LoadingRecord(it, taskRecordLoader, userCustomTimeProviderSource)
            }

            val newMap = (oldMap + newEntries).toMutableMap()
            check(newMap.getValue(taskRecord.taskKey) is TaskLoadState.LoadingRecord)

            newMap[taskRecord.taskKey] =
                    TaskLoadState.LoadingTimes(taskRecord, userCustomTimeProviderSource)

            return newMap
        }
    }

    class TimesLoadedMutator(private val taskRecord: RootTaskRecord) : TaskLoadStateMapMutator {

        override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
            check(oldMap.containsKey(taskRecord.taskKey))

            return oldMap.toMutableMap().also { it[taskRecord.taskKey] = TaskLoadState.Loaded(taskRecord.taskKey) }
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