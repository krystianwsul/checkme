package com.krystianwsul.common.domain

import com.krystianwsul.common.utils.ProjectKey

class ProjectUndoData {

    val projectIds = mutableSetOf<ProjectKey<*>>()
    val taskUndoData = TaskUndoData()
}