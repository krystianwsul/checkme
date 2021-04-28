package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.utils.doOnSuccessOrDispose
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.observables.ConnectableObservable
import io.reactivex.rxjava3.observables.GroupedObservable

class RootTaskFactory(
        loadDependencyTrackerManager: LoadDependencyTrackerManager,
        private val rootTaskToRootTaskCoordinator: RootTaskToRootTaskCoordinator,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val rootTasksFactory: RootTasksFactory,
        private val domainDisposable: CompositeDisposable,
        addChangeEvents: GroupedObservable<TaskKey.Root, RootTasksLoader.AddChangeEvent>,
) {

    private val taskKey = addChangeEvents.key

    private val removeRelay = PublishRelay.create<Unit>()
    fun onRemove() = removeRelay.accept(Unit)

    private val eventResults: ConnectableObservable<EventResult>

    val changeTypes: ConnectableObservable<ChangeType>
    val unfilteredChanges: ConnectableObservable<Unit>

    var task: RootTask? = null
        private set

    private data class AddChangeData(val task: RootTask, val isTracked: Boolean)

    init {
        eventResults = listOf(
                addChangeEvents.map { Event.AddChange(it.rootTaskRecord, it.isTracked) },
                removeRelay.map { Event.Remove },
        ).merge()
                .switchMapSingle { event ->
                    when (event) {
                        is Event.AddChange -> {
                            val (taskRecord, isTracked) = event

                            val taskTracker = loadDependencyTrackerManager.startTrackingTaskLoad(taskRecord)

                            Singles.zip(
                                    rootTaskToRootTaskCoordinator.getRootTasks(taskRecord).toSingleDefault(Unit),
                                    rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord),
                            ).doOnSuccessOrDispose {
                                taskTracker.stopTracking() // in case a new event comes in before this completes
                            }.map { (_, userCustomTimeProvider) ->
                                EventResult.SetTask(
                                        RootTask(taskRecord, rootTasksFactory, userCustomTimeProvider),
                                        isTracked,
                                )
                            }
                        }
                        is Event.Remove -> Single.just(EventResult.RemoveTask)
                    }
                }
                .doOnNext { task = it.task }
                .publish()

        val unfilteredSetTaskEventResults = eventResults.ofType<EventResult.SetTask>()

        val addChangeEventChanges = unfilteredSetTaskEventResults.filter { !it.isTracked }

        /**
         * order is important: the bottom one executes later, and we first need to check filtering before emitting the
         * unfiltered event
         *
         * We don't include removeEventChangeTypes here, since those will be emitted in the process of updating whatever
         * initially requested the task.
         */

        changeTypes = addChangeEventChanges.map { ChangeType.REMOTE }.publish()
        unfilteredChanges = unfilteredSetTaskEventResults.map { }.publish()
    }

    private var connected = false

    fun connect() {
        if (connected) return
        connected = true

        domainDisposable += changeTypes.connect()
        domainDisposable += unfilteredChanges.connect()

        domainDisposable += eventResults.connect()
    }

    private sealed class Event {

        data class AddChange(val rootTaskRecord: RootTaskRecord, val isTracked: Boolean) : Event()

        object Remove : Event()
    }

    private sealed class EventResult {

        abstract val task: RootTask?

        class SetTask(override val task: RootTask, val isTracked: Boolean) : EventResult()

        object RemoveTask : EventResult() {

            override val task: RootTask? = null
        }
    }
}