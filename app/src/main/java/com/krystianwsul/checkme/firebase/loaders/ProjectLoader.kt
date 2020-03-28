package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class ProjectLoader<T : ProjectType>(
        projectRecordObservable: Observable<ProjectRecord<*>>,
        domainDisposable: CompositeDisposable,
        factoryProvider: FactoryProvider
) {

    init {
        val rootInstanceDatabaseRx = projectRecordObservable.map { it to it.taskRecords.mapKeys { it.value.taskKey } }
                .processChanges(
                        { it.second.keys },
                        { (_, newData), taskKey ->
                            val taskRecord = newData.getValue(taskKey)

                            Pair(
                                    taskRecord,
                                    DatabaseRx(
                                            domainDisposable,
                                            factoryProvider.database.getRootInstanceObservable(taskRecord.rootInstanceKey)
                                    )
                            )
                        },
                        { it.second.disposable.dispose() }
                )
                .replay(1)
                .apply { domainDisposable += connect() }

        val addProjectEvent = rootInstanceDatabaseRx.firstOrError().flatMap {
            val projectRecord = it.first.first

            it.second
                    .newMap
                    .values
                    .map { (taskRecord, databaseRx) ->
                        databaseRx.first.map { taskRecord.taskKey to it.toSnapshotInfos() }
                    }
                    .zipSingle()
                    .map { AddProjectEvent<T>(projectRecord, it.toMap()) }
        }
    }

    class AddProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<*>,
            val snapshotInfos: Map<TaskKey, List<AndroidRootInstanceManager.SnapshotInfo>>
    )
}