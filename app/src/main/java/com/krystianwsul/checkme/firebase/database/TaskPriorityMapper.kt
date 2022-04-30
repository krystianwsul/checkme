package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.common.utils.TaskKey

interface TaskPriorityMapper {

    fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead): DatabaseReadPriority

    object Default : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead) =
            taskDatabaseRead.getTaskPriority().databaseReadPriority
    }

    class PrioritizeTaskWithDependencies private constructor(val taskKey: TaskKey.Root) : TaskPriorityMapper {

        companion object {

            fun fromTaskKey(taskKey: TaskKey) = taskKey.let { it as? TaskKey.Root }?.let(::PrioritizeTaskWithDependencies)
        }

        private val allDependentTaskKeys = mutableSetOf<TaskKey.Root>()

        init {
            allDependentTaskKeys += taskKey

            RootTasksFactory.nullableInstance?.let { addTaskDependencies(it, taskKey) }
        }

        private fun addTaskDependencies(
            rootTasksFactory: RootTasksFactory,
            taskKey: TaskKey.Root,
            checkedTaskKeys: MutableSet<TaskKey.Root> = mutableSetOf(),
        ) {
            if (taskKey in checkedTaskKeys) return
            checkedTaskKeys += taskKey

            val task = rootTasksFactory.getRootTaskIfPresent(taskKey) ?: return

            val dependencyTaskKeys = task.taskRecord.getAllDependencyTaskKeys()

            allDependentTaskKeys += dependencyTaskKeys

            dependencyTaskKeys.forEach { addTaskDependencies(rootTasksFactory, it, checkedTaskKeys) }
        }

        override fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead): DatabaseReadPriority {
            return if (taskDatabaseRead.taskKey in allDependentTaskKeys)
                DatabaseReadPriority.TODAY_INSTANCES
            else
                Default.getDatabaseReadPriority(taskDatabaseRead)
        }
    }
}