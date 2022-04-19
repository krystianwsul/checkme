package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.common.utils.TaskKey

interface TaskPriorityMapper {

    fun getDatabaseReadPriority(taskPriority: HasInstancesStore.TaskPriority): DatabaseReadPriority

    object Default : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskPriority: HasInstancesStore.TaskPriority) =
            taskPriority.databaseReadPriority
    }

    class PrioritizeTask(val taskKey: TaskKey.Root) : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskPriority: HasInstancesStore.TaskPriority) =
            DatabaseReadPriority.TODAY_INSTANCES
    }
}