package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.mapValuesNotNull
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksFactory(
        val rootTasksLoader: RootTasksLoader,
        private val userKeyStore: UserKeyStore,
        private val rootTaskDependencyCoordinator: RootTaskDependencyCoordinator,
        domainDisposable: CompositeDisposable,
        private val rootTaskKeySource: RootTaskKeySource,
        loadDependencyTrackerManager: LoadDependencyTrackerManager,
        private val getProjectsFactory: () -> ProjectsFactory,
) : RootTask.Parent {

    private val rootTaskFactoriesObservable: Observable<Map<TaskKey.Root, RootTaskFactory>>

    val rootTasks: Map<TaskKey.Root, RootTask>
        get() = rootTaskFactoriesObservable.getCurrentValue().mapValuesNotNull { it.value.task }

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
                                rootTaskDependencyCoordinator,
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
        TODO("todo task after edit")
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

    fun newTask(taskJson: RootTaskJson): RootTask {
        val taskKey = rootTasksLoader.addTask(taskJson)

        return getRootTask(taskKey)
    }

    override fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
    ) = newTask(RootTaskJson(name, now.long, now.offset, note = note, image = image, ordinal = ordinal))

    override fun updateProjectRecord(projectKey: ProjectKey<*>, dependentRootTaskKeys: Set<TaskKey.Root>) {
        rootTasksLoader.ignoreKeyUpdates {
            rootTaskKeySource.onProjectAddedOrUpdated(projectKey, dependentRootTaskKeys)
        }
    }

    override fun updateProject(taskKey: TaskKey.Root, oldProject: Project<*>, newProjectKey: ProjectKey<*>) {
        val newProject = getProjectsFactory().getProjectForce(newProjectKey)

        rootTasksLoader.ignoreKeyUpdates {
            oldProject.removeRootTask(taskKey)
            newProject.addRootTask(taskKey)
        }
    }
}