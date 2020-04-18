package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign

interface ProjectLoader<T : ProjectType> {

    // first snapshot of everything
    val initialProjectEvent: Single<ChangeWrapper<InitialProjectEvent<T>>>

    // Here we observe the initial instances for new tasks
    val addTaskEvents: Observable<ChangeWrapper<AddTaskEvent<T>>>

    // Here we observe changes to all the previously subscribed instances
    val changeInstancesEvents: Observable<ChangeWrapper<ChangeInstancesEvent<T>>>

    // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
    val changeProjectEvents: Observable<ChangeWrapper<ChangeProjectEvent<T>>>

    class InitialProjectEvent<T : ProjectType>(
            val projectManager: ProjectProvider.ProjectManager<T>,
            val projectRecord: ProjectRecord<T>,
            val snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
    )

    class AddTaskEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    )

    class ChangeInstancesEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val snapshotInfos: List<AndroidRootInstanceManager.SnapshotInfo>
    )

    class ChangeProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
    )

    class Impl<T : ProjectType>(
            snapshotObservable: Observable<Snapshot>,
            private val domainDisposable: CompositeDisposable,
            projectProvider: ProjectProvider,
            private val projectManager: ProjectProvider.ProjectManager<T>
    ) : ProjectLoader<T> {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

        private val projectRecordObservable = snapshotObservable.mapNotNull { projectManager.setProjectRecord(it) }

        private data class ProjectData<T : ProjectType>(
                val changeWrapper: ChangeWrapper<out ProjectRecord<T>>,
                val taskMap: Map<TaskKey, TaskRecord<T>>
        )

        private data class InstanceData<T : ProjectType>(
                val taskRecord: TaskRecord<T>,
                val databaseRx: DatabaseRx
        )

        private val rootInstanceDatabaseRx = projectRecordObservable.map {
            ProjectData(
                    it,
                    it.data
                            .taskRecords
                            .mapKeys { it.value.taskKey }
            )
        }
                .processChanges(
                        { it.taskMap.keys },
                        { (_, newData), taskKey ->
                            val taskRecord = newData.getValue(taskKey)

                            InstanceData(
                                    taskRecord,
                                    DatabaseRx(
                                            domainDisposable,
                                            projectProvider.database.getRootInstanceObservable(taskRecord.rootInstanceKey)
                                    )
                            )
                        },
                        { it.databaseRx.disposable.dispose() }
                )
                .replayImmediate()

        // first snapshot of everything
        override val initialProjectEvent = rootInstanceDatabaseRx.firstOrError()
                .flatMap {
                    val (changeType, projectRecord) = it.original.changeWrapper

                    it.newMap
                            .values
                            .map { (taskRecord, databaseRx) ->
                                databaseRx.first.map { taskRecord.taskKey to it.toSnapshotInfos() }
                            }
                            .zipSingle()
                            .map { ChangeWrapper(changeType, InitialProjectEvent(projectManager, projectRecord, it.toMap())) }
                }
                .cache()
                .apply { domainDisposable += subscribe() }!!

        // Here we observe the initial instances for new tasks
        override val addTaskEvents = rootInstanceDatabaseRx.skip(1)
                .switchMap {
                    val (changeType, projectRecord) = it.original.changeWrapper

                    it.addedEntries
                            .values
                            .map { (taskRecord, databaseRx) ->
                                databaseRx.first
                                        .toObservable()
                                        .map {
                                            ChangeWrapper(
                                                    changeType,
                                                    AddTaskEvent(projectRecord, taskRecord, it.toSnapshotInfos())
                                            )
                                        }
                            }
                            .merge()
                }
                .replayImmediate()

        // Here we observe changes to all the previously subscribed instances
        override val changeInstancesEvents = rootInstanceDatabaseRx.switchMap {
            val (changeType, projectRecord) = it.original.changeWrapper

            it.newMap
                    .values
                    .map { (taskRecord, databaseRx) ->
                        databaseRx.changes.map {
                            ChangeWrapper(changeType, ChangeInstancesEvent(projectRecord, taskRecord, it.toSnapshotInfos()))
                        }
                    }
                    .merge()
        }.replayImmediate()

        // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
        override val changeProjectEvents = rootInstanceDatabaseRx.skip(1)
                .filter { it.addedEntries.isEmpty() }
                .switchMapSingle {
                    val (changeType, projectRecord) = it.original.changeWrapper

                    it.newMap
                            .values
                            .let {
                                if (it.isEmpty()) {
                                    Single.just(ChangeProjectEvent(projectRecord, mapOf()))
                                } else {
                                    it.map { (taskRecord, databaseRx) ->
                                        databaseRx.latest().map { taskRecord.taskKey to it }
                                    }
                                            .zipSingle()
                                            .map {
                                                ChangeProjectEvent(
                                                        projectRecord,
                                                        it.toMap().mapValues { it.value.toSnapshotInfos() }
                                                )
                                            }
                                }
                            }.map { ChangeWrapper(changeType, it) }
                }
                .replayImmediate()
    }
}