package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey

class JsRootTasksManager(databaseWrapper: DatabaseWrapper, rootTaskJsons: Map<String, RootTaskJson>) :
        RootTasksManager(databaseWrapper) {

    init {
        setInitialRecords(
                rootTaskJsons.entries.associate {
                    val taskKey = TaskKey.Root(it.key)
                    taskKey to RootTaskRecord(it.key, it.value, databaseWrapper, this)
                }
        )
    }
}