package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize

data class SearchCriteria(
    val search: Search? = null,
    val showAssignedToOthers: Boolean = true,
    val showDone: Boolean = true,
    val excludedInstanceKeys: Set<InstanceKey> = setOf(),
) {

    companion object {

        val empty = SearchCriteria()
    }

    val isEmpty by lazy { this == empty }

    sealed interface Search : Parcelable {

        val hasSearch: Boolean
        val expandMatches get() = hasSearch

        @Parcelize
        data class Query(val query: String = "") : Search {

            override val hasSearch get() = query.isNotEmpty()
        }

        @Parcelize
        data class TaskKey(val taskKey: com.krystianwsul.common.utils.TaskKey) : Search {

            override val hasSearch get() = true
        }
    }
}