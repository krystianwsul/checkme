package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
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

class ProjectLoader<T : ProjectType>(
        snapshotObservable: Observable<Snapshot>,
        private val domainDisposable: CompositeDisposable,
        projectProvider: ProjectProvider,
        private val projectManager: ProjectProvider.ProjectManager<T>
) {

    private fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }!!

    // todo instances feed this event back into domainFactory. use another event emitter in all record managers?

    /*
    todo instances Considering that unsaved tasks and instances are just added to the data model, I'm strongly
    considering adding dummy projects as well.  That means that these events should go through the rest of the
    pipeline, to properly set up database listeners and what-now.  That would mean ditching this nullability,
    and moving the isSaved checks somewhere downstream... I think?  Though it would be nice to nip the whole isSaved
    thing well ahead, ideally even before parsing to *Json if possible.  If that's not an option, then I don't really
    see a difference between nipping local events at the record vs. model stages, may as well go all the way if that
    makes the code simpler; as long as I can feed the ChangeType into the DomainFactory correctly.
     */
    private val projectRecordObservable = snapshotObservable.mapNotNull { projectManager.setProjectRecord(it) } // todo changewrapper

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
            .publishImmediate()

    // first snapshot of everything
    val initialProjectEvent = rootInstanceDatabaseRx.firstOrError()
            .flatMap {
                val projectRecord = it.first.first

                it.second
                        .newMap
                        .values
                        .map { (taskRecord, databaseRx) ->
                            databaseRx.first.map { taskRecord.taskKey to it.toSnapshotInfos() }
                        }
                        .zipSingle()
                        .map { InitialProjectEvent(projectManager, projectRecord, it.toMap()) }
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
            .publishImmediate()

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
    }.publishImmediate()

    // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
    val changeProjectEvents = rootInstanceDatabaseRx.skip(1)
            .filter { it.second.addedEntries.isEmpty() }
            .switchMapSingle {
                val projectRecord = it.first.first

                it.second
                        .newMap
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
                        }
            }
            .publishImmediate()

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
}