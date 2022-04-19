package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.utils.TaskKey

class TaskDatabaseRead(private val taskKey: TaskKey.Root) : TypedDatabaseRead<RootTaskJson>() {

    override fun getPriority() = HasInstancesStore.getTaskPriority(taskKey).databaseReadPriority

    override val kClass = RootTaskJson::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.TASKS_KEY}/${taskKey.taskId}")
}