package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.utils.TaskKey

class TaskDatabaseRead(private val taskKey: TaskKey.Root) : TypedDatabaseRead<RootTaskJson>() {

    private var finalTaskPriority: HasInstancesStore.TaskPriority.Final? = null

    private fun getTaskPriority(): HasInstancesStore.TaskPriority {
        finalTaskPriority?.let { return it }

        val taskPriority = HasInstancesStore.getTaskPriority(taskKey)

        taskPriority.let { it as? HasInstancesStore.TaskPriority.Final }?.let { finalTaskPriority = it }

        return taskPriority
    }

    override fun getPriority(taskPriorityMapper: TaskPriorityMapper) =
        taskPriorityMapper.getDatabaseReadPriority(getTaskPriority())

    override val kClass = RootTaskJson::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.TASKS_KEY}/${taskKey.taskId}")
}