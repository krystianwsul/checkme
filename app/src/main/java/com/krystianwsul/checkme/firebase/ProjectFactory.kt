package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.firebase.reduce
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

@Suppress("LeakingThis")
abstract class ProjectFactory<T : ProjectType>(
        projectLoader: ProjectLoader<T>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<T>,
        protected val factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable
) {

    private val projectManager = initialProjectEvent.projectManager

    protected var rootInstanceManagers: MutableMap<TaskKey, AndroidRootInstanceManager<T>>
        private set

    var project: Project<T>
        private set

    val isSaved get() = projectManager.isSaved || rootInstanceManagers.values.any { it.isSaved }

    private fun newRootInstanceManagers(
            projectRecord: ProjectRecord<T>,
            snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
    ) = projectRecord.taskRecords
            .values
            .map {
                it.taskKey to AndroidRootInstanceManager(
                        it,
                        snapshotInfos.getValue(it.taskKey),
                        factoryProvider
                )
            }
            .toMap()
            .toMutableMap()

    protected fun newRootInstanceManager(
            taskRecord: TaskRecord<T>,
            snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    ): AndroidRootInstanceManager<T> {
        check(!rootInstanceManagers.containsKey(taskRecord.taskKey))

        return AndroidRootInstanceManager(taskRecord, snapshotInfos, factoryProvider).apply {
            rootInstanceManagers[taskRecord.taskKey] = this
        }
    }

    protected abstract fun newProject(projectRecord: ProjectRecord<T>): Project<T>

    init {
        rootInstanceManagers = newRootInstanceManagers(initialProjectEvent.projectRecord, initialProjectEvent.snapshotInfos)
        project = newProject(initialProjectEvent.projectRecord)

        fun updateRootInstanceManager(
                taskRecord: TaskRecord<T>,
                snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
        ): ChangeType {
            val rootInstanceManager = rootInstanceManagers[taskRecord.taskKey]

            return if (rootInstanceManager != null) {
                rootInstanceManager.setSnapshotInfos(snapshotInfos)
            } else {
                newRootInstanceManager(taskRecord, snapshotInfos)

                ChangeType.REMOTE
            }
        }

        /*
        todo instances feed REMOTE into DomainFactory when there might be fresh data coming in from
        Firebase, which might entail 1. changed reminders, and therefore a notification sound, or
        2. new data, requiring an update to domain listeners.  So pretty much #2.
         */

        projectLoader.changeProjectEvents
                .subscribe { (projectChangeType, changeProjectEvent) ->
                    val instanceChangeType = changeProjectEvent.projectRecord
                            .taskRecords
                            .values
                            .map {
                                updateRootInstanceManager(it, changeProjectEvent.snapshotInfos.getValue(it.taskKey))
                            }
                            .reduce()

                    project = newProject(changeProjectEvent.projectRecord)

                    val changeType = ChangeType.reduce(projectChangeType, instanceChangeType) // todo instances local/remote
                }
                .addTo(domainDisposable)

        projectLoader.addTaskEvents
                .subscribe { (projectChangeType, addTaskEvent) ->
                    val instanceChangeType = addTaskEvent.run { updateRootInstanceManager(taskRecord, snapshotInfos) }

                    project = newProject(addTaskEvent.projectRecord)

                    val changeType = ChangeType.reduce(projectChangeType, instanceChangeType) // todo instances local/remote
                }
                .addTo(domainDisposable)

        projectLoader.changeInstancesEvents
                .subscribe { (projectChangeType, changeInstancesEvent) ->
                    val instanceChangeType = changeInstancesEvent.run {
                        updateRootInstanceManager(taskRecord, snapshotInfos)
                    }

                    project = newProject(changeInstancesEvent.projectRecord)

                    val changeType = ChangeType.reduce(projectChangeType, instanceChangeType) // todo instances local/remote
                }
                .addTo(domainDisposable)
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val projectSaved = projectManager.save(domainFactory)
        val instancesSaved = rootInstanceManagers.map { it.value.save() }.any { it }
        return projectSaved || instancesSaved
    }
}