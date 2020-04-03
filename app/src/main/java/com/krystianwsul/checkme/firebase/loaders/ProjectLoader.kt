package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign

class ProjectLoader<T : ProjectType>(
        snapshotObservable: Observable<FactoryProvider.Database.Snapshot>,
        domainDisposable: CompositeDisposable,
        projectProvider: ProjectProvider,
        projectManager: ProjectProvider.ProjectManager<T>
) {

    private val projectRecordObservable = snapshotObservable.map(projectManager::addProjectRecord)

    private val rootInstanceDatabaseRx = projectRecordObservable.map { it to it.taskRecords.mapKeys { it.value.taskKey } }
            .processChanges(
                    { it.second.keys },
                    { (_, newData), taskKey ->
                        val taskRecord = newData.getValue(taskKey)

                        Pair(
                                taskRecord,
                                DatabaseRx(
                                        domainDisposable,
                                        projectProvider.database.getRootInstanceObservable(taskRecord.rootInstanceKey)
                                )
                        )
                    },
                    { it.second.disposable.dispose() }
            )
            .publish()
            .apply { domainDisposable += connect() }

    // first snapshot of everything
    val addProjectEvent = rootInstanceDatabaseRx.firstOrError()
            .flatMap {
                val projectRecord = it.first.first

                it.second
                        .newMap
                        .values
                        .map { (taskRecord, databaseRx) ->
                            databaseRx.first.map { taskRecord.taskKey to it.toSnapshotInfos() }
                        }
                        .zipSingle()
                        .map { AddProjectEvent(projectManager, projectRecord, it.toMap()) }
            }
            .cache()
            .apply { domainDisposable += subscribe() }!!

    // Here we observe the initial instances for new tasks
    val addTaskEvents = rootInstanceDatabaseRx.skip(1)
            .switchMap {
                val projectRecord = it.first.first

                it.second
                        .addedEntries
                        .values
                        .map { (taskRecord, databaseRx) ->
                            databaseRx.first
                                    .toObservable()
                                    .map { AddTaskEvent(projectRecord, taskRecord, it.toSnapshotInfos()) }
                        }
                        .merge()
            }
            .publish()
            .apply { domainDisposable += connect() }!!

    // Here we observe changes to all the previously subscribed instances
    val changeInstancesEvents = rootInstanceDatabaseRx.switchMap {
        val projectRecord = it.first.first

        it.second
                .newMap
                .values
                .map { (taskRecord, databaseRx) ->
                    databaseRx.changes.map {
                        ChangeInstancesEvent(projectRecord, taskRecord, it.toSnapshotInfos())
                    }
                }
                .merge()
    }
            .publish()
            .apply { domainDisposable += connect() }!!

    val changeProjectEvents = rootInstanceDatabaseRx.skip(1)
            .filter { it.second.addedEntries.isEmpty() }
            .switchMap {
                val projectRecord = it.first.first

                it.second
                        .newMap
                        .values
                        .map { (_, databaseRx) ->
                            databaseRx.latest()
                                    .map { ChangeProjectEvent(projectRecord, it.toSnapshotInfos()) }
                                    .toObservable()
                        }
                        .merge()
            }
            .publish()
            .apply { domainDisposable += connect() }!!

    class AddProjectEvent<T : ProjectType>(
            val projectManager: ProjectProvider.ProjectManager<T>,
            val projectRecord: ProjectRecord<T>,
            val snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
    )

    class AddTaskEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val snapshotInfo: List<AndroidRootInstanceManager.SnapshotInfo>
    )

    class ChangeInstancesEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val snapshotInfo: List<AndroidRootInstanceManager.SnapshotInfo>
    )

    class ChangeProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val snapshotInfo: List<AndroidRootInstanceManager.SnapshotInfo>
    )
}