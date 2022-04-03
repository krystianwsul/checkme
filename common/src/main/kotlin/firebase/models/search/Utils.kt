package com.krystianwsul.common.firebase.models.search

import android.util.Log
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.interrupt.InterruptionChecker

// todo searchContext private
fun childHierarchyMatches(task: Task, searchContext: SearchContext, onlyHierarchy: Boolean = false): FilterResult {
    InterruptionChecker.throwIfInterrupted()

    return task.getMatchResult(searchContext.searchCriteria.search).let {
        it.getFilterResult() ?: run {
            if (searchContext.searchingChildrenOfQueryMatch) {
                FilterResult.Include(false)
            } else {
                val childTasks = if (onlyHierarchy) task.getHierarchyChildTasks() else task.getChildTasks()

                if (childTasks.any { !childHierarchyMatches(it, searchContext, onlyHierarchy).doesntMatch })
                    FilterResult.Include(false)
                else
                    FilterResult.Exclude
            }
        }
    }
}

// todo taskKey remove
private fun Task.getChain(chain: MutableList<String>) {
    chain.add(0, name)

    if (isTopLevelTask()) {
        chain.add(0, project.name)
    } else {
        parentTask!!.getChain(chain)
    }
}

fun logFilterResult(task: Task, filterResult: FilterResult) {
    val chain = mutableListOf<String>()
    task.getChain(chain)

    Log.e("asdf", "magic " + chain.filter { it.isNotEmpty() }.joinToString("/") + " filterResult: " + filterResult)
}