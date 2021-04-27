package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
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

    val changeTypes: Observable<ChangeType>
    val unfilteredChanges: Observable<Unit>

    var task: RootTask? = null
        private set

    private data class AddChangeData(val task: RootTask, val isTracked: Boolean)

    init {
        val unfilteredAddChangeEventChanges = addChangeEvents.flatMapSingle { (taskRecord, isTracked) ->
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

            task = null
        }

        /**
         * order is important: the bottom one executes later, and we first need to check filtering before emitting the
         * unfiltered event
         *
         * We don't include removeEventChangeTypes here, since those will be emitted in the process of updating whatever
         * initially requested the task.
         */

        // todo task track I'm almost sure this isn't correct, since the map in RootTasksFactory won't pick up the initial event
        changeTypes = addChangeEventChanges.map { ChangeType.REMOTE }.publishImmediate(domainDisposable)

        unfilteredChanges = unfilteredAddChangeEventChanges.map { }.replay(1)
                .apply { domainDisposable += connect() }

        unfilteredAddChangeEventChanges.connect()
    }
}