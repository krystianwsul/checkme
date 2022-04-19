package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.domainmodel.HasInstancesStore

interface TaskPriorityMapper {

    fun getDatabaseReadPriority(taskPriority: HasInstancesStore.TaskPriority): DatabaseReadPriority

    class Default : TaskPriorityMapper {

        override fun getDatabaseReadPriority(taskPriority: HasInstancesStore.TaskPriority) =
            taskPriority.databaseReadPriority
    }
}