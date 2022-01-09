package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Ordinal
import com.krystianwsul.common.utils.toFields
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.mapValuesNotNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksFactory(
    private val rootTasksLoader: RootTasksLoader,
    private val userKeyStore: UserKeyStore,
    private val rootTaskDependencyCoordinator: RootTaskDependencyCoordinator,
    domainDisposable: CompositeDisposable,
    private val rootTaskKeySource: RootTaskKeySource,
    override val rootModelChangeManager: RootModelChangeManager,
    private val getProjectsFactory: () -> ProjectsFactory,
) : RootTask.Parent {

    private val deletedKeys = mutableSetOf<TaskKey.Root>()

    private val rootTaskFactoriesRelay = BehaviorRelay.create<Map<TaskKey.Root, RootTaskFactory>>()
    private val rootTaskFactories get() = rootTaskFactoriesRelay.value!!

    val rootTasks: Map<TaskKey.Root, RootTask>
        get() {
            return if (Notifier.TEST_IRRELEVANT)
                rootTaskFactories.filterKeys { it !in deletedKeys }.mapValuesNotNull { it.value.task }
            else
                rootTaskFactories.mapValuesNotNull { it.value.task }
        }

    val changeTypes: Observable<ChangeType>

    init {
        rootTasksLoader.addChangeEvents
            .groupBy { it.rootTaskRecord.taskKey }
            .scan(mapOf<TaskKey.Root, RootTaskFactory>()) { oldMap, group ->
                check(!oldMap.containsKey(group.key!!))

                oldMap.toMutableMap().also {
                    it[group.key!!] = RootTaskFactory(
                        rootTaskDependencyCoordinator,
                        this,
                        domainDisposable,
                        group,
                        rootModelChangeManager,
                    )
                }
            }
            .subscribe(rootTaskFactoriesRelay)
            .addTo(domainDisposable)

        rootTasksLoader.removeEvents
            .subscribe {
                it.taskKeys.forEach { rootTaskFactories[it]?.onRemove() }

                userKeyStore.onTasksRemoved(it.taskKeys)
                rootTaskKeySource.onRootTasksRemoved(it.taskKeys)
            }
            .addTo(domainDisposable)

        changeTypes = rootTaskFactoriesRelay.switchMap {
            it.map { it.value.changeTypes }.merge()
        }.publishImmediate(domainDisposable)

        domainDisposable += rootTaskFactoriesRelay.subscribe {
            it.forEach { it.value.connect() }
        }
    }

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> {
        return rootTasks.flatMap { it.value.nestedParentTaskHierarchies.values }
            .filter { it.parentTaskKey == parentTaskKey }
            .toSet()
    }

    override fun deleteRootTask(task: RootTask) {
        if (Notifier.TEST_IRRELEVANT)
            deletedKeys += task.taskKey
        else
            throw UnsupportedOperationException()
    }

    fun getRootTaskIfPresent(taskKey: TaskKey.Root) = rootTasks[taskKey]

    override fun tryGetRootTask(rootTaskKey: TaskKey.Root) = rootTasks[rootTaskKey]

    override fun getProject(projectId: String) = getProjectsFactory().getProjectForce(projectId)

    override fun getTask(taskKey: TaskKey): Task {
        return when (taskKey) {
            is TaskKey.Root -> getRootTask(taskKey)
            is TaskKey.Project -> getProjectsFactory().getProjectTaskForce(taskKey)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun getAllExistingInstances() = getProjectsFactory().projects
        .values
        .asSequence()
        .flatMap { it.getAllDependenciesLoadedTasks() }
        .flatMap { it.existingInstances.values }

    override fun getRootTasksForProject(projectKey: ProjectKey<*>) =
        rootTasks.values.filter { it.dependenciesLoaded && it.projectId == projectKey.key }

    fun newTask(taskJson: RootTaskJson): RootTask {
        val taskKey = rootTasksLoader.addTask(taskJson)

        return getRootTask(taskKey)
    }

    override fun createTask(
        now: ExactTimeStamp.Local,
        image: TaskJson.Image?,
        name: String,
        note: String?,
        ordinal: Ordinal?,
    ): RootTask {
        val (ordinalDouble, ordinal128) = ordinal.toFields()

        return newTask(
            RootTaskJson(
                name,
                now.long,
                now.offset,
                note = note,
                image = image,
                ordinal = ordinalDouble,
                ordinal128 = ordinal128,
            )
        )
    }

    override fun updateProjectRecord(projectKey: ProjectKey<*>, dependentRootTaskKeys: Set<TaskKey.Root>) {
        rootTasksLoader.ignoreKeyUpdates {
            rootTaskKeySource.onProjectAddedOrUpdated(projectKey, dependentRootTaskKeys)
        }
    }

    override fun updateTaskRecord(taskKey: TaskKey.Root, dependentRootTaskKeys: Set<TaskKey.Root>) {
        rootTasksLoader.ignoreKeyUpdates {
            rootTaskKeySource.onRootTaskAddedOrUpdated(taskKey, dependentRootTaskKeys)
        }
    }

    fun save(values: MutableMap<String, Any?>) = rootTasksLoader.rootTasksManager.save(values)
}