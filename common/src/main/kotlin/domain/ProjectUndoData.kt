package com.krystianwsul.common.domain

import com.krystianwsul.common.utils.ProjectKey

class ProjectUndoData {

    val projectKeys = mutableSetOf<ProjectKey.Shared>()
    val taskUndoData = TaskUndoData()
}