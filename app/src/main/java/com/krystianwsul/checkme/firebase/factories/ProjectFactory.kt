package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

@Suppress("LeakingThis")
abstract class ProjectFactory<T : ProjectType, U : Parsable>(
// U: Project JSON type
        projectLoader: ProjectLoader<T, U>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<T, U>,
        protected val factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        protected val deviceDbInfo: () -> DeviceDbInfo,
) {

    private val projectManager = initialProjectEvent.projectManager

    protected val rootInstanceManagers: MutableMap<TaskKey, AndroidRootInstanceManager<T>>

    var project: Project<T>
        private set

    val isSaved get() = projectManager.isSaved || rootInstanceManagers.values.any { it.isSaved }

    val savedList get() = projectManager.savedList + rootInstanceManagers.values.flatMap { it.savedList }

    private fun newRootInstanceManagers(
            projectRecord: ProjectRecord<T>,
            snapshots: Map<TaskKey, Snapshot<Map<String, Map<String, InstanceJson>>>>,
    ) = projectRecord.taskRecords
        .values
        .associate {
            it.taskKey to AndroidRootInstanceManager(
                it,
                snapshots.getValue(it.taskKey),
                factoryProvider
            )
        }
            .toMutableMap()

    protected fun newRootInstanceManager(
            taskRecord: TaskRecord<T>,
            snapshot: Snapshot<Map<String, Map<String, InstanceJson>>>?,
    ): AndroidRootInstanceManager<T> {
        check(!rootInstanceManagers.containsKey(taskRecord.taskKey))

        return AndroidRootInstanceManager(taskRecord, snapshot, factoryProvider).apply {
            rootInstanceManagers[taskRecord.taskKey] = this
        }
    }

    protected abstract fun newProject(projectRecord: ProjectRecord<T>): Project<T>

    val changeTypes: Observable<ChangeType>

    init {
        rootInstanceManagers = initialProjectEvent.run { newRootInstanceManagers(projectRecord, instanceSnapshots) }
        project = newProject(initialProjectEvent.projectRecord)

        fun updateRootInstanceManager(taskRecord: TaskRecord<T>, snapshot: Snapshot<Map<String, Map<String, InstanceJson>>>): ChangeType {
            val rootInstanceManager = rootInstanceManagers[taskRecord.taskKey]

            return if (rootInstanceManager != null) {
                rootInstanceManager.set(snapshot).changeType
            } else {
                newRootInstanceManager(taskRecord, snapshot)

                ChangeType.REMOTE
            }
        }

        val changeProjectChangeTypes = projectLoader.changeProjectEvents.map { (projectChangeType, changeProjectEvent) ->
            changeProjectEvent.projectRecord
                    .taskRecords
                    .values
                    .forEach { updateRootInstanceManager(it, changeProjectEvent.instanceSnapshots.getValue(it.taskKey)) }

            project = newProject(changeProjectEvent.projectRecord)

            projectChangeType
        }

        val addTaskChangeTypes = projectLoader.addTaskEvents.map { (projectChangeType, addTaskEvent) ->
            addTaskEvent.apply { updateRootInstanceManager(taskRecord, instanceSnapshot) }

            check(rootInstanceManagers.containsKey(addTaskEvent.taskRecord.taskKey))

            project = newProject(addTaskEvent.projectRecord)

            projectChangeType
        }

        val changeInstancesChangeTypes = projectLoader.changeInstancesEvents.map { changeInstancesEvent ->
            val instanceChangeType = changeInstancesEvent.run { updateRootInstanceManager(taskRecord, instanceSnapshot) }

            project = newProject(changeInstancesEvent.projectRecord)

            instanceChangeType
        }

        changeTypes = listOf(
                changeProjectChangeTypes,
                addTaskChangeTypes,
                changeInstancesChangeTypes
        ).merge().publishImmediate(domainDisposable)
    }

    fun saveInstances(values: MutableMap<String, Any?>) = rootInstanceManagers.forEach { it.value.save(values) }
}