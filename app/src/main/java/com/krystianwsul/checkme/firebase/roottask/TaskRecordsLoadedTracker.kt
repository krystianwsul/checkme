package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

interface TaskRecordsLoadedTracker {

    fun getTaskRecordSingle(taskKey: TaskKey.Root): Single<RootTaskRecord>

    fun tryGetTaskRecord(taskKey: TaskKey.Root): RootTaskRecord?

    class Impl(private val rootTasksLoader: RootTasksLoader, domainDisposable: CompositeDisposable) :
            TaskRecordsLoadedTracker {

        private val currentlyLoadedRecords = mutableMapOf<TaskKey.Root, RootTaskRecord>()

        init {
            rootTasksLoader.addChangeEvents
                    .subscribe { currentlyLoadedRecords[it.rootTaskRecord.taskKey] = it.rootTaskRecord }
                    .addTo(domainDisposable)

            rootTasksLoader.removeEvents
                    .subscribe { currentlyLoadedRecords -= it.taskKeys }
                    .addTo(domainDisposable)
        }

        override fun getTaskRecordSingle(taskKey: TaskKey.Root): Single<RootTaskRecord> {
            currentlyLoadedRecords[taskKey]?.let { return Single.just(it) }

            return rootTasksLoader.addChangeEvents
                    .filter { it.rootTaskRecord.taskKey == taskKey }
                    .firstOrError()
                    .map { it.rootTaskRecord }
        }

        override fun tryGetTaskRecord(taskKey: TaskKey.Root) = currentlyLoadedRecords[taskKey]
    }
}