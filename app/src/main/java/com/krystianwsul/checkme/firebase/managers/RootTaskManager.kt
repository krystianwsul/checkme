package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.managers.MapRecordManager
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey

class RootTaskManager : MapRecordManager<TaskKey.Root, RootTaskRecord>() {

    override val databasePrefix = DatabaseWrapper.TASKS_KEY

    override fun valueToRecord(value: RootTaskRecord) = value
}