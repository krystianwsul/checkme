package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.utils.doOnSuccessOrDispose
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.observables.ConnectableObservable
import io.reactivex.rxjava3.observables.GroupedObservable

class RootTaskFactory(
    loadDependencyTrackerManager: LoadDependencyTrackerManager,
    private val rootTaskDependencyCoordinator: RootTaskDependencyCoordinator,
    private val rootTasksFactory: RootTasksFactory,
    private val domainDisposable: CompositeDisposable,
    addChangeEvents: GroupedObservable<TaskKey.Root, RootTasksLoader.AddChangeEvent>,
    private val rootTaskDependencyStateContainer: RootTaskDependencyStateContainer,
    private val rootModelChangeManager: RootModelChangeManager,
) {

    private val taskKey = addChangeEvents.key

    private val removeRelay = PublishRelay.create<Unit>()
    fun onRemove() = removeRelay.accept(Unit)

    private val eventResults: ConnectableObservable<EventResult>

    val changeTypes: ConnectableObservable<ChangeType>
    val unfilteredChanges: ConnectableObservable<Unit>

    var task: RootTask? = null
        private set

    init {
        eventResults = listOf(
            addChangeEvents.map { Event.AddChange(it.rootTaskRecord, it.skipChangeTypeEmission) },
            removeRelay.map { Event.Remove },
        ).merge()
            .switchMapSingle { event ->
                when (event) {
                    is Event.AddChange -> {
                        val (taskRecord, skipChangeTypeEmission) = event

                        val taskTracker = loadDependencyTrackerManager.startTrackingTaskLoad(taskRecord)

                        rootTaskDependencyCoordinator.getDependencies(taskRecord)
                            .doOnSuccessOrDispose { taskTracker.stopTracking() } // in case a new event comes in before this completes
                            .map {
                                EventResult.SetTask(
                                    RootTask(taskRecord, rootTasksFactory, it),
                                    skipChangeTypeEmission,
                                )
                            }
                    }
                    is Event.Remove -> Single.just(EventResult.RemoveTask)
                }
            }
            .doOnNext {
                task?.clearableInvalidatableManager?.clear()

                rootModelChangeManager.invalidateRootTasks()

                task = it.task

                rootTaskDependencyStateContainer.apply {
                    if (it.task != null) {
                        onLoaded(it.task!!.taskRecord)
                    } else {
                        onRemoved(taskKey)
                    }
                }
            }
            .publish()

        val unfilteredSetTaskEventResults = eventResults.ofType<EventResult.SetTask>()

        val addChangeEventChangeTypes = unfilteredSetTaskEventResults.filter { !it.skipChangeTypeEmission }

        /**
         * order is important: the bottom one executes later, and we first need to check filtering before emitting the
         * unfiltered event
         *
         * We don't include removeEventChangeTypes here, since those will be emitted in the process of updating whatever
         * initially requested the task.
         */

        changeTypes = addChangeEventChangeTypes.map { ChangeType.REMOTE }.publish()
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

        data class AddChange(val rootTaskRecord: RootTaskRecord, val skipChangeTypeEmission: Boolean) : Event()

        object Remove : Event()
    }

    private sealed class EventResult {

        abstract val task: RootTask?

        class SetTask(override val task: RootTask, val skipChangeTypeEmission: Boolean) : EventResult()

        object RemoveTask : EventResult() {

            override val task: RootTask? = null
        }
    }
}