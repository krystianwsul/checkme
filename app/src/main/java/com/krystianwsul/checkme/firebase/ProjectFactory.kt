package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
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

    protected val projectManager = initialProjectEvent.projectManager

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

    protected fun newRootInstanceManager( // todo instances factor isSaved into all these
            taskRecord: TaskRecord<T>,
            snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    ): AndroidRootInstanceManager<T> {
        check(!rootInstanceManagers.containsKey(taskRecord.taskKey))

        return setRootInstanceManager(taskRecord, snapshotInfos)
    }

    private fun setRootInstanceManager(
            taskRecord: TaskRecord<T>,
            snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    ) = AndroidRootInstanceManager(taskRecord, snapshotInfos, factoryProvider).apply {
        rootInstanceManagers[taskRecord.taskKey] = this
    }

    protected abstract fun newProject(projectRecord: ProjectRecord<T>): Project<T>

    init {
        // todo instances figure out if I should be checking isSaved for all this

        rootInstanceManagers = newRootInstanceManagers(initialProjectEvent.projectRecord, initialProjectEvent.snapshotInfos)
        project = newProject(initialProjectEvent.projectRecord)

        projectLoader.changeProjectEvents
                .subscribe {
                    check(rootInstanceManagers.values.none { it.isSaved })

                    rootInstanceManagers = newRootInstanceManagers(it.projectRecord, it.snapshotInfos)
                    project = newProject(it.projectRecord)
                }
                .addTo(domainDisposable)

        projectLoader.addTaskEvents
                .subscribe {
                    check(rootInstanceManagers[it.taskRecord.taskKey]?.isSaved != true)

                    newRootInstanceManager(it.taskRecord, it.snapshotInfos)
                    project = newProject(it.projectRecord)
                }
                .addTo(domainDisposable)

        projectLoader.changeInstancesEvents
                .subscribe {
                    if (rootInstanceManagers[it.taskRecord.taskKey]?.isSaved == true) {
                        // todo clear isSaved
                    } else {
                        setRootInstanceManager(it.taskRecord, it.snapshotInfos)
                        project = newProject(it.projectRecord)
                    }
                }
                .addTo(domainDisposable)
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val projectSaved = projectManager.save(domainFactory)
        val instancesSaved = rootInstanceManagers.map { it.value.save() }.any { it }
        return projectSaved || instancesSaved
    }
}