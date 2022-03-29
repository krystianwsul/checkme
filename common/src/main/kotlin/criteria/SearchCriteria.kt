package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize

// todo optimization: split up into smaller classes and use empty/isEmpty model for them, to optimize sub-searches
data class SearchCriteria(
    val search: Search? = null,
    val showAssignedToOthers: Boolean = true,
    val showDone: Boolean = true, // this is definitely not the same as showDeleted
    val excludedInstanceKeys: Set<InstanceKey> = setOf(),
) {

    companion object {

        val empty = SearchCriteria()
    }

    val isEmpty by lazy { this == empty }

    sealed interface Search : Parcelable {

        val hasSearch: Boolean

        val expandMatches get() = hasSearch

        val needsNormalization: Boolean

        @Parcelize
        data class Query(val query: String = "") : Search {

            override val hasSearch get() = query.isNotEmpty()

            override val needsNormalization get() = hasSearch
        }

        @Parcelize
        data class TaskKey(val taskKey: com.krystianwsul.common.utils.TaskKey) : Search {

            override val hasSearch get() = true

            override val needsNormalization get() = false
        }
    }
}