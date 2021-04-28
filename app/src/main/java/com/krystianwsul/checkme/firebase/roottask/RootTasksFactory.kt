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
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

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

    private val rootTaskFactoriesObservable: Observable<Map<TaskKey.Root, RootTaskFactory>>

    val rootTasks: Map<TaskKey.Root, RootTask>
        get() = rootTaskFactoriesObservable.getCurrentValue()
                .mapValues { it.value.task }
                .filterValues { it != null }
                .mapValues { it.value!! }

    val unfilteredChanges: Observable<Unit>
    val changeTypes: Observable<ChangeType>

    init {
        rootTaskFactoriesObservable = rootTasksLoader.addChangeEvents
                .groupBy { it.rootTaskRecord.taskKey }
                .scan(mapOf<TaskKey.Root, RootTaskFactory>()) { oldMap, group ->
                    check(!oldMap.containsKey(group.key))

                    oldMap.toMutableMap().also {
                        it[group.key] = RootTaskFactory(
                                loadDependencyTrackerManager,
                                rootTaskToRootTaskCoordinator,
                                rootTaskUserCustomTimeProviderSource,
                                this,
                                domainDisposable,
                                group,
                        )
                    }
                }
                .replay(1)

        domainDisposable += rootTaskFactoriesObservable.connect()

        fun getFactory(taskKey: TaskKey.Root) = rootTaskFactoriesObservable.getCurrentValue()[taskKey]

        val removeEvents = rootTasksLoader.removeEvents
                .doOnNext {
                    it.taskKeys.forEach { getFactory(it)?.onRemove() }

                    userKeyStore.onTasksRemoved(it.taskKeys)
                    rootTaskKeySource.onRootTasksRemoved(it.taskKeys)
                }

        changeTypes = rootTaskFactoriesObservable.switchMap {
            it.map { it.value.changeTypes }.merge()
        }.publishImmediate(domainDisposable)

        val factoryUnfilteredChanges = rootTaskFactoriesObservable.switchMap {
            it.map { it.value.unfilteredChanges }.merge()
        }

        unfilteredChanges =
                listOf(factoryUnfilteredChanges, removeEvents.map { }).merge().publishImmediate(domainDisposable)

        domainDisposable += rootTaskFactoriesObservable.subscribe {
            it.forEach { it.value.connect() }
        }
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