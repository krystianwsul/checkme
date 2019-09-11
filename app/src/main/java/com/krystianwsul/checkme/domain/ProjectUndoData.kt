package com.krystianwsul.checkme.domain

class ProjectUndoData {

    val projectIds = mutableSetOf<String>()
    val taskUndoData = TaskUndoData()
}