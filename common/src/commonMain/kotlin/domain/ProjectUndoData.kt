package com.krystianwsul.common.domain

class ProjectUndoData {

    val projectIds = mutableSetOf<String>()
    val taskUndoData = TaskUndoData()
}