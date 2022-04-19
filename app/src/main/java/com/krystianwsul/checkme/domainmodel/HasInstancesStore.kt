package com.krystianwsul.checkme.domainmodel

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.firebase.database.DatabaseReadPriority
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.kotlin.subscribeBy

object HasInstancesStore {

    private const val KEY = "hasInstancesKey"

    // lazy for tests
    private val book by lazy { RxPaperBook.with("hasInstances2") }

    private class Data(val fromFile: Boolean, val taskData: TaskData?)

    private class TaskData(val date: Date, val map: Map<TaskKey.Root, InstanceState>)

    private val hasInstancesMapRelay = BehaviorRelay.create<Data>()

    fun init() {
        book.read<TaskData>(KEY)
            .toV3()
            .onErrorComplete()
            .subscribeBy {
                if (!hasInstancesMapRelay.hasValue()) hasInstancesMapRelay.accept(Data(true, it))
            }

        hasInstancesMapRelay.filter { !it.fromFile }
            .toFlowable(BackpressureStrategy.LATEST)
            .flatMapCompletable(
                { book.write(KEY, it.taskData).toV3() },
                false,
                1,
            )
            .subscribe()
    }

    private fun calculateHasInstances(task: RootTask, now: ExactTimeStamp.Local) =
        task.getInstances(null, null, now).firstOrNull().let {
            when {
                it == null -> InstanceState.NONE
                it.instanceDate <= now.date -> InstanceState.TODAY
                else -> InstanceState.ANY
            }
        }

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

        val hasInstancesMap = tasks.mapValues { InstanceState.NONE }.toMutableMap()

        tasks.map { it.value to calculateHasInstances(it.value, now) }
            .filter { it.second > InstanceState.NONE }
            .forEach { setTaskPriority(it.first, it.second, hasInstancesMap) }

        hasInstancesMapRelay.accept(Data(false, TaskData(now.date, hasInstancesMap)))
    }

    fun getTaskPriority(taskKey: TaskKey.Root): TaskPriority {
        val taskData = hasInstancesMapRelay.value
            ?.taskData
            ?: return TaskPriority.TODAY_INSTANCES

        return when (taskData.map.getOrDefault(taskKey, InstanceState.NONE)) {
            InstanceState.NONE -> TaskPriority.NOTES
            // if we have yesterday's data, then we'd better load all instances
            InstanceState.ANY -> if (taskData.date == Date.today())
                TaskPriority.LATER_INSTANCES
            else
                TaskPriority.TODAY_INSTANCES
            InstanceState.TODAY -> TaskPriority.TODAY_INSTANCES
        }
    }

    private fun setTaskPriority(task: RootTask, instanceState: InstanceState, map: MutableMap<TaskKey.Root, InstanceState>) {
        if (map.getOrDefault(task.taskKey, InstanceState.NONE) >= instanceState) return

        map[task.taskKey] = instanceState

        task.rootTaskDependencyResolver
            .directDependencyTasks
            .forEach { setTaskPriority(it, instanceState, map) }
    }

    private enum class InstanceState {

        NONE, ANY, TODAY
    }

    enum class TaskPriority(val databaseReadPriority: DatabaseReadPriority) {

        NOTES(DatabaseReadPriority.NOTES),
        LATER_INSTANCES(DatabaseReadPriority.LATER_INSTANCES),
        TODAY_INSTANCES(DatabaseReadPriority.TODAY_INSTANCES)
    }
}