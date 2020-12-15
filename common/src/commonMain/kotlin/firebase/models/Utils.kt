package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.QueryData

fun <T : ProjectType> Sequence<Task<out T>>.filterQuery(queryData: QueryData) = if (queryData.query.isNotEmpty()) {
    fun filterQuery(task: Task<out T>): FilterResult {
        throwIfInterrupted()

        if (task.matchesQueryData(queryData)) return FilterResult.MATCHES

        if (task.childHierarchyIntervals.any { filterQuery(it.taskHierarchy.childTask) != FilterResult.DOESNT_MATCH })
            return FilterResult.CHILD_MATCHES

        return FilterResult.DOESNT_MATCH
    }

    map { it to filterQuery(it) }.filter { it.second != FilterResult.DOESNT_MATCH }
} else {
    map { it to FilterResult.MATCHES }
}

fun <T : ProjectType> Sequence<Instance<out T>>.filterQuery(
        queryData: QueryData,
        now: ExactTimeStamp.Local,
        myUser: MyUser,
) = if (queryData.query.isNotEmpty() || !queryData.showAssigned) {
    fun filterQuery(instance: Instance<out T>): FilterResult {
        throwIfInterrupted()

        if (instance.matchesQueryData(queryData, now, myUser)) return FilterResult.MATCHES

        if (instance.getChildInstances(now).any { filterQuery(it.first) != FilterResult.DOESNT_MATCH })
            return FilterResult.CHILD_MATCHES

        return FilterResult.DOESNT_MATCH
    }

    map { it to filterQuery(it) }.filter { it.second != FilterResult.DOESNT_MATCH }
} else {
    map { it to FilterResult.MATCHES }
}

enum class FilterResult {

    DOESNT_MATCH, CHILD_MATCHES, MATCHES
}