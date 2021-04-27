package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.merge

class RootTasksFactory(
        rootTasksLoader: RootTasksLoader,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val userKeyStore: UserKeyStore,
        private val rootTaskToRootTaskCoordinator: RootTaskToRootTaskCoordinator,
        domainDisposable: CompositeDisposable,
        private val rootTaskKeySource: RootTaskKeySource,
        loadDependencyTrackerManager: LoadDependencyTrackerManager,
        private val getProjectsFactory: () -> ProjectsFactory,
) : RootTask.Parent {

    private val rootTaskMap = mutableMapOf<TaskKey.Root, RootTask>()

    val rootTasks: Map<TaskKey.Root, RootTask> get() = rootTaskMap

    val unfilteredChanges: Observable<Unit>
    val changeTypes: Observable<ChangeType>

    private data class AddChangeData(val task: RootTask, val isTracked: Boolean)

    init {
        val unfilteredAddChangeEventChanges = rootTasksLoader.addChangeEvents
                .switchMapSingle { (taskRecord, isTracked) ->
                    val taskTracker = loadDependencyTrackerManager.startTrackingTaskLoad(taskRecord)

                    Singles.zip(
                            rootTaskToRootTaskCoordinator.getRootTasks(taskRecord).toSingleDefault(Unit),
                            rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord),
                    ).map { (_, userCustomTimeProvider) ->
                        taskTracker.stopTracking()

                        AddChangeData(RootTask(taskRecord, this, userCustomTimeProvider), isTracked)
                    }
                }
                .doOnNext { rootTaskMap[it.task.taskKey] = it.task }
                .share()

        val addChangeEventChanges = unfilteredAddChangeEventChanges.filter { !it.isTracked }

        val removeEventChanges = rootTasksLoader.removeEvents
                .doOnNext {
                    it.taskKeys.forEach {
                        check(rootTaskMap.containsKey(it))

                        rootTaskMap.remove(it)
                    }

                    userKeyStore.onTasksRemoved(it.taskKeys)
                    rootTaskKeySource.onRootTasksRemoved(it.taskKeys)
                }
                .share()

        /**
         * order is important: the bottom one executes later, and we first need to check filtering before emitting the
         * unfiltered event
         *
         * We don't include removeEventChangeTypes here, since those will be emitted in the process of updating whatever
         * initially requested the task.
         */
        changeTypes = addChangeEventChanges.map { ChangeType.REMOTE }.publishImmediate(domainDisposable)

        unfilteredChanges = listOf(
                unfilteredAddChangeEventChanges,
                removeEventChanges,
        ).merge()
                .map { }
                .publishImmediate(domainDisposable)
    }

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> {
        return rootTasks.flatMap { it.value.nestedParentTaskHierarchies.values }
                .filter { it.parentTaskKey == parentTaskKey }
                .toSet()
    }

    override fun deleteRootTask(task: RootTask) {
        TODO("todo task edit")
    }

    fun getRootTaskIfPresent(taskKey: TaskKey.Root) = rootTasks[taskKey]

    override fun getRootTask(rootTaskKey: TaskKey.Root) = rootTasks.getValue(rootTaskKey)

    override fun getProject(projectId: String) = getProjectsFactory().getProjectForce(projectId)

    override fun getTask(taskKey: TaskKey): Task {
        return when (taskKey) {
            is TaskKey.Root -> getRootTask(taskKey)
            is TaskKey.Project -> getProjectsFactory().getProjectTaskForce(taskKey)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun getRootTasksForProject(projectKey: ProjectKey<*>) =
            rootTasks.values.filter { it.projectId == projectKey.key }
}