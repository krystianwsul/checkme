package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

interface ProjectLoader<T : ProjectType, U : Parsable> { // U: Project JSON type

    val projectManager: ProjectProvider.ProjectManager<T, U>

    // first snapshot of everything
    val initialProjectEvent: Single<ChangeWrapper<InitialProjectEvent<T, U>>>

    // Here we observe the initial instances for new tasks
    val addTaskEvents: Observable<ChangeWrapper<AddTaskEvent<T>>>

    // Here we observe changes to all the previously subscribed instances
    val changeInstancesEvents: Observable<ChangeInstancesEvent<T>>

    // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
    val changeProjectEvents: Observable<ChangeWrapper<ChangeProjectEvent<T>>>

    class InitialProjectEvent<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            val projectManager: ProjectProvider.ProjectManager<T, U>,
            val projectRecord: ProjectRecord<T>,
            val instanceSnapshots: Map<TaskKey, IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>>,
    )

    class AddTaskEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val instanceSnapshot: IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>,
    )

    class ChangeInstancesEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val taskRecord: TaskRecord<T>,
            val instanceSnapshot: IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>,
    )

    class ChangeProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val instanceSnapshots: Map<TaskKey, IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>>,
    )

    class Impl<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            snapshotObservable: Observable<TypedSnapshot<U>>,
            private val domainDisposable: CompositeDisposable,
            projectProvider: ProjectProvider,
            override val projectManager: ProjectProvider.ProjectManager<T, U>,
    ) : ProjectLoader<T, U> {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

        private val projectRecordObservable = snapshotObservable.mapNotNull { projectManager.set(it) }

        private data class ProjectData<T : ProjectType>(
                val changeWrapper: ChangeWrapper<out ProjectRecord<T>>,
                val taskMap: Map<TaskKey, TaskRecord<T>>,
        )

        private data class InstanceData<T : ProjectType>(
                val taskRecord: TaskRecord<T>,
                val databaseRx: DatabaseRx<IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>>,
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
                                databaseRx.first.map { taskRecord.taskKey to it }
                            }
                            .zipSingle()
                            .map {
                                ChangeWrapper(
                                        changeType,
                                        InitialProjectEvent(projectManager, projectRecord, it.toMap()),
                                )
                            }
                }
                .cacheImmediate(domainDisposable)

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
                                                    AddTaskEvent(projectRecord, taskRecord, it)
                                            )
                                        }
                            }
                            .merge()
                }
                .replayImmediate()

        // Here we observe changes to all the previously subscribed instances
        override val changeInstancesEvents = rootInstanceDatabaseRx.switchMap {
            val projectRecord = it.original
                    .changeWrapper
                    .data

            it.newMap
                    .values
                    .map { (taskRecord, databaseRx) ->
                        databaseRx.changes.map {
                            ChangeInstancesEvent(projectRecord, taskRecord, it)
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
                                                        it.toMap().mapValues { it.value },
                                                )
                                            }
                                }
                            }.map { ChangeWrapper(changeType, it) }
                }
                .replayImmediate()
    }
}