package com.krystianwsul.checkme.firebase.loaders

import com.badoo.reaktive.rxjavainterop.asRxJava3Observable
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
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
    val addTaskEvents: Observable<ChangeWrapper<AddTaskEvent<T>>> // todo instance

    // Here we observe changes to all the previously subscribed instances
    val changeInstancesEvents: Observable<ChangeInstancesEvent<T>> // todo instance

    // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
    val changeProjectEvents: Observable<ChangeWrapper<ChangeProjectEvent<T>>>

    class InitialProjectEvent<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            val projectManager: ProjectProvider.ProjectManager<T, U>,
            val projectRecord: ProjectRecord<T>,
    )

    class AddTaskEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
    )

    class ChangeInstancesEvent<T : ProjectType>

    class ChangeProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
    )

    class Impl<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            snapshotObservable: Observable<Snapshot<U>>,
            private val domainDisposable: CompositeDisposable,
            override val projectManager: ProjectProvider.ProjectManager<T, U>,
            initialProjectRecord: ProjectRecord<T>?,
    ) : ProjectLoader<T, U> {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

        private val projectRecordObservable = snapshotObservable.mapNotNull { projectManager.set(it) }.let {
            if (initialProjectRecord != null) {
                it.startWithItem(ChangeWrapper(ChangeType.LOCAL, initialProjectRecord))
            } else {
                it
            }
        }

        private data class ProjectData<T : ProjectType>(
                val internallyUpdated: Boolean,
                val changeWrapper: ChangeWrapper<out ProjectRecord<T>>,
                val taskMap: Map<TaskKey, TaskRecord<T>>,
        )

        private data class InstanceData<T : ProjectType>(
                val internallyUpdated: Boolean,
                val taskRecord: TaskRecord<T>,
                val databaseRx: DatabaseRx<ProjectProvider.RootInstanceData>,
        )

        private val rootInstanceDatabaseRx = projectRecordObservable.switchMap { changeWrapper ->
            val taskObservable = changeWrapper.data
                    .taskRecordsRelay
                    .asRxJava3Observable()
                    .map {
                        it.mapKeys { it.value.taskKey }
                    }
                    .share()

            listOf(
                    taskObservable.take(1).map { ProjectData(false, changeWrapper, it) },
                    taskObservable.skip(1).map { ProjectData(true, changeWrapper, it) }
            ).merge()
        }
                .processChanges(
                        { it.taskMap.keys },
                        { (internallyUpdated, _, newData), taskKey ->
                            val taskRecord = newData.getValue(taskKey)

                            InstanceData(
                                    internallyUpdated,
                                    taskRecord,
                                    DatabaseRx(
                                            domainDisposable,
                                            Observable.just(ProjectProvider.RootInstanceData(false, Snapshot("", null))), // todo instances
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
                            .map { (_, taskRecord, databaseRx) ->
                                databaseRx.first.map { taskRecord.taskKey to it }
                            }
                            .zipSingle()
                            .map {
                                ChangeWrapper(
                                        changeType,
                                        InitialProjectEvent(
                                                projectManager,
                                                projectRecord,
                                        ),
                                )
                            }
                }
                .cacheImmediate(domainDisposable)

        // Here we observe the initial instances for new tasks
        override val addTaskEvents = rootInstanceDatabaseRx.skip(1)
                .switchMap {
                    val (originalChangeType, projectRecord) = it.original.changeWrapper

                    it.addedEntries
                            .values
                            .map { (internallyUpdated, _, databaseRx) ->
                                databaseRx.first
                                        .toObservable()
                                        .map {
                                            val changeType = if (internallyUpdated) {
                                                if (it.rootEnabled) {
                                                    originalChangeType
                                                } else {
                                                    ChangeType.LOCAL
                                                }
                                            } else {
                                                originalChangeType
                                            }

                                            ChangeWrapper(
                                                    changeType,
                                                    AddTaskEvent(projectRecord)
                                            )
                                        }
                            }
                            .merge()
                }
                .replayImmediate()

        // Here we observe changes to all the previously subscribed instances
        override val changeInstancesEvents = rootInstanceDatabaseRx.switchMap {
            it.newMap
                    .values
                    .map { (_, _, databaseRx) ->
                        databaseRx.changes.map {
                            check(it.rootEnabled)

                            ChangeInstancesEvent<T>()
                        }
                    }
                    .merge()
        }.replayImmediate()

        // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
        override val changeProjectEvents = rootInstanceDatabaseRx.skip(1)
                .filter { it.addedEntries.isEmpty() }
                .switchMapSingle {
                    val (changeType, projectRecord) = it.original.changeWrapper

                    check(changeType == ChangeType.REMOTE)

                    it.newMap
                            .values
                            .let {
                                if (it.isEmpty()) {
                                    Single.just(ChangeProjectEvent(projectRecord))
                                } else {
                                    it.map { (_, taskRecord, databaseRx) ->
                                        databaseRx.latest().map { taskRecord.taskKey to it }
                                    }
                                            .zipSingle()
                                            .map {
                                                ChangeProjectEvent(projectRecord)
                                            }
                                }
                            }.map { ChangeWrapper(changeType, it) }
                }
                .replayImmediate()
    }
}