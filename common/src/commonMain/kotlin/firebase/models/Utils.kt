package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.utils.ProjectType

fun <T : ProjectType> Sequence<Task<out T>>.filterQuery(query: String?) = query?.let {
    fun filterQuery(task: Task<out T>): Boolean {
        throwIfInterrupted()

        if (task.matchesQuery(it)) return true

        return task.childHierarchyIntervals.any { filterQuery(it.taskHierarchy.childTask) }
    }

    filter(::filterQuery)
} ?: this