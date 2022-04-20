package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.common.utils.TaskKey

interface TaskPriorityMapper {

    fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead): DatabaseReadPriority

    object Default : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead) =
            taskDatabaseRead.getTaskPriority().databaseReadPriority
    }

    class PrioritizeTask(val taskKey: TaskKey.Root) : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead) = DatabaseReadPriority.TODAY_INSTANCES
    }

    // todo priority rename to something sane
    class PrioritizeTask2(val taskKey: TaskKey.Root) : TaskPriorityMapper {

        // todo priority I should distinguish between a mapper object, and a mapping... session?  For this test, just initialize it each time.  Or maybe a factory vs. actual mapper

        val allDependentTaskKeys = mutableSetOf<TaskKey.Root>()

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

            dependencyTaskKeys.forEach { addTaskDependencies(rootTasksFactory, taskKey, checkedTaskKeys) }
        }

        override fun getDatabaseReadPriority(taskDatabaseRead: TaskDatabaseRead): DatabaseReadPriority {
            return if (taskDatabaseRead.taskKey in allDependentTaskKeys)
                DatabaseReadPriority.NORMAL
            else
                DatabaseReadPriority.LATER_INSTANCES
        }
    }
}