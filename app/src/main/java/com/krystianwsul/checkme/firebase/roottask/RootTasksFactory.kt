package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.checkme.firebase.dependencies.UserKeyStore
import com.krystianwsul.checkme.firebase.factories.OwnedProjectsFactory
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectCoordinator
import com.krystianwsul.checkme.firebase.foreignProjects.ForeignProjectsFactory
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class RootTasksFactory(
    private val rootTasksLoader: RootTasksLoader,
    private val userKeyStore: UserKeyStore,
    private val rootTaskDependencyCoordinator: RootTaskDependencyCoordinator,
    domainDisposable: CompositeDisposable,
    private val rootTaskKeyStore: RootTaskKeyStore,
    override val rootModelChangeManager: RootModelChangeManager,
    private val foreignProjectCoordinator: ForeignProjectCoordinator,
    private val foreignProjectsFactory: ForeignProjectsFactory,
    private val shownFactorySingle: Single<Instance.ShownFactory>,
    private val getProjectsFactory: () -> OwnedProjectsFactory,
) : RootTask.Parent {

    companion object {

        var allowDeletion = Notifier.TEST_IRRELEVANT

        val instanceRelay = BehaviorRelay.createDefault(NullableWrapper<RootTasksFactory>())

        val nullableInstance get() = instanceRelay.value!!.value
    }

    private val deletedKeys = mutableSetOf<TaskKey.Root>()

    private val rootTaskFactoriesRelay = BehaviorRelay.create<Map<TaskKey.Root, RootTaskFactory>>()
    private val rootTaskFactories get() = rootTaskFactoriesRelay.value!!

    val rootTasks: Map<TaskKey.Root, RootTask>
        get() {
            return if (allowDeletion)
                rootTaskFactories.filterKeys { it !in deletedKeys }.mapValuesNotNull { it.value.task }
            else
                rootTaskFactories.mapValuesNotNull { it.value.task }
        }

    val remoteChanges: Observable<Unit>

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
                        foreignProjectCoordinator,
                        shownFactorySingle,
                    )
                }
            }
            .subscribe(rootTaskFactoriesRelay)
            .addTo(domainDisposable)

        rootTasksLoader.removeEvents
            .subscribe {
                it.taskKeys.forEach { rootTaskFactories[it]?.onRemove() }

                userKeyStore.onTasksRemoved(it.taskKeys)
                rootTaskKeyStore.onRootTasksRemoved(it.taskKeys)
            }
            .addTo(domainDisposable)

        remoteChanges = rootTaskFactoriesRelay.switchMap {
            it.map { it.value.remoteChanges }.merge()
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
        if (allowDeletion)
            deletedKeys += task.taskKey
        else
            throw UnsupportedOperationException()
    }

    fun getRootTaskIfPresent(taskKey: TaskKey.Root) = rootTasks[taskKey]

    override fun tryGetRootTask(rootTaskKey: TaskKey.Root) = rootTasks[rootTaskKey]

    override fun getOwnedProjectIfPresent(projectId: String) = getProjectsFactory().getProjectByIdIfPresent(projectId)

    override fun getProjectIfPresent(projectKey: ProjectKey<*>): Project<*>? {
        return getProjectsFactory().getProjectIfPresent(projectKey) ?: foreignProjectsFactory.getProjectIfPresent(projectKey)
    }

    override fun getTask(taskKey: TaskKey): Task {
        return when (taskKey) {
            is TaskKey.Root -> getRootTask(taskKey)
            is TaskKey.Project -> getProjectsFactory().getProjectTaskForce(taskKey)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun getAllExistingInstances(): Sequence<Instance> {
        val allTasks = getProjectsFactory().projectTasks + rootTasks.values

        return allTasks.asSequence().flatMap { it.existingInstances.values }
    }

    override fun getRootTasks() = rootTasks.values

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
        val (ordinalDouble, ordinalString) = ordinal.toFields()

        return newTask(
            RootTaskJson(
                name,
                now.long,
                now.offset,
                note = note,
                image = image,
                ordinal = ordinalDouble,
                ordinalString = ordinalString,
            )
        )
    }

    override fun updateProjectRecord(projectKey: ProjectKey<*>, dependentRootTaskKeys: Set<TaskKey.Root>) {
        rootTasksLoader.ignoreKeyUpdates {
            rootTaskKeyStore.onProjectAddedOrUpdated(projectKey, dependentRootTaskKeys)
        }
    }

    override fun updateTaskRecord(taskKey: TaskKey.Root, dependentRootTaskKeys: Set<TaskKey.Root>) {
        rootTasksLoader.ignoreKeyUpdates {
            rootTaskKeyStore.onRootTaskAddedOrUpdated(taskKey, dependentRootTaskKeys)
        }
    }

    fun save(values: MutableMap<String, Any?>) = rootTasksLoader.rootTasksManager.save(values)

    fun waitForTaskLoad(taskKey: TaskKey.Root): Completable {
        return rootTaskFactoriesRelay.mapNotNull { it[taskKey] }
            .distinctUntilChanged()
            .flatMap { it.taskRelay.filterNotNull() }
            .firstOrError()
            .ignoreElement()
    }
}