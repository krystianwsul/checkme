package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.mindorks.scheduler.Priority
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.kotlin.subscribeBy

object HasInstancesStore {

    private const val KEY = "hasInstancesKey"

    private val book = RxPaperBook.with("hasInstances")

    private class Data(val fromFile: Boolean, val map: Map<TaskKey.Root, Boolean>)

    private val hasInstancesMapRelay = BehaviorRelay.create<Data>()

    fun init() {
        book.read(KEY, mapOf<TaskKey.Root, Boolean>())
            .toV3()
            .subscribeBy {
                if (!hasInstancesMapRelay.hasValue()) hasInstancesMapRelay.accept(Data(true, it))
            }

        hasInstancesMapRelay.filter { !it.fromFile }
            .toFlowable(BackpressureStrategy.LATEST)
            .flatMapCompletable(
                { book.write(KEY, it.map).toV3() },
                false,
                1,
            )
            .subscribe()
    }

    private fun calculateHasInstances(task: RootTask, now: ExactTimeStamp.Local) =
        task.getInstances(null, null, now).any()

    fun update(domainFactory: DomainFactory, now: ExactTimeStamp.Local) {
        val projectsNullable = domainFactory.myUserFactory
            .user
            .let { it.projectIds + it.userKey.toPrivateProjectKey() }
            .map { domainFactory.projectsFactory.getProjectIfPresent(it) }

        if (null in projectsNullable) return

        val projects = projectsNullable.requireNoNulls()

        val tasksNullable = projects.flatMap { it.projectRecord.rootTaskParentDelegate.rootTaskKeys }
            .toSet()
            .associateWith { domainFactory.rootTasksFactory.getRootTaskIfPresent(it) }

        if (null in tasksNullable.values) return

        val tasks = tasksNullable.mapValues { it.value!! }

        if (tasks.values.any { !it.dependenciesLoaded }) return

        val hasInstancesMap = tasks.mapValues { calculateHasInstances(it.value, now) }

        hasInstancesMapRelay.accept(Data(false, hasInstancesMap))
    }

    fun getPriority(taskKey: TaskKey.Root): Priority {
        val map = hasInstancesMapRelay.value
            ?.map
            ?: return Priority.DB_TASKS // if the file hasn't loaded

        val hasInstances = map[taskKey] ?: return Priority.DB_NOTES // if it's not in the cache, wait on it

        return if (hasInstances) Priority.DB_TASKS else Priority.DB_NOTES
    }
}