package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.utils.TaskKey

class TaskDatabaseRead(private val taskKey: TaskKey.Root) : TypedDatabaseRead<RootTaskJson>() {

    override val type = "task"

    override val priority get() = HasInstancesStore.getPriority(taskKey)

    override val kClass = RootTaskJson::class

    override fun getResult() =
        AndroidDatabaseWrapper.rootReference.child("${DatabaseWrapper.TASKS_KEY}/${taskKey.taskId}").typedSnapshotChanges()
}