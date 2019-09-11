package com.krystianwsul.checkme.domain

import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey

class TaskUndoData {

    val taskKeys = mutableSetOf<TaskKey>()
    val scheduleIds = mutableSetOf<ScheduleId>()
    val taskHierarchyKeys = mutableSetOf<TaskHierarchyKey>()
}