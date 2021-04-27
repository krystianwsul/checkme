package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.observables.ConnectableObservable
import io.reactivex.rxjava3.observables.GroupedObservable

class RootTaskFactory(
        loadDependencyTrackerManager: LoadDependencyTrackerManager,
        private val rootTaskToRootTaskCoordinator: RootTaskToRootTaskCoordinator,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val rootTasksFactory: RootTasksFactory,
        domainDisposable: CompositeDisposable,
        addChangeEvents: GroupedObservable<TaskKey.Root, RootTasksLoader.AddChangeEvent>,
) {

    private val taskKey = addChangeEvents.key

    private val removeRelay = PublishRelay.create<Unit>()
    fun onRemove() = removeRelay.accept(Unit)

    private val unfilteredAddChangeEventChanges: ConnectableObservable<AddChangeData>

    val changeTypes: ConnectableObservable<ChangeType>
    val unfilteredChanges: ConnectableObservable<Unit>

    var task: RootTask? = null
        private set

    private data class AddChangeData(val task: RootTask, val isTracked: Boolean)

    init {
        unfilteredAddChangeEventChanges = addChangeEvents.flatMapSingle { (taskRecord, isTracked) ->
            val taskTracker = loadDependencyTrackerManager.startTrackingTaskLoad(taskRecord)

            Singles.zip(
                    rootTaskToRootTaskCoordinator.getRootTasks(taskRecord).toSingleDefault(Unit),
                    rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord),
            ).map { (_, userCustomTimeProvider) ->
                taskTracker.stopTracking()

                AddChangeData(RootTask(taskRecord, rootTasksFactory, userCustomTimeProvider), isTracked)
            }
        }
                .doOnNext { task = it.task }
                .publish()

        val addChangeEventChanges = unfilteredAddChangeEventChanges.filter { !it.isTracked }

        domainDisposable += removeRelay.subscribe {
            checkNotNull(task)
            check(connected)

            task = null
        }

        /**
         * order is important: the bottom one executes later, and we first need to check filtering before emitting the
         * unfiltered event
         *
         * We don't include removeEventChangeTypes here, since those will be emitted in the process of updating whatever
         * initially requested the task.
         */

        changeTypes = addChangeEventChanges.map { ChangeType.REMOTE }.publish()
        unfilteredChanges = unfilteredAddChangeEventChanges.map { }.publish()
    }

    private var connected = false

    fun connect() {
        if (connected) return
        connected = true

        changeTypes.connect()
        unfilteredChanges.connect()

        unfilteredAddChangeEventChanges.connect()
    }
}