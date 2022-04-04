package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize

// todo optimization: split up into smaller classes and use empty/isEmpty model for them, to optimize sub-searches
data class SearchCriteria(
    val commonCriteria: CommonCriteria = CommonCriteria.empty,
    val taskCriteria: TaskCriteria = TaskCriteria.empty,
    val instanceCriteria: InstanceCriteria = InstanceCriteria.empty,
) {

    companion object {

        val empty = SearchCriteria()
    }

    constructor(
        search: Search = CommonCriteria.empty.search,
        showAssignedToOthers: Boolean = CommonCriteria.empty.showAssignedToOthers,
        showDone: Boolean = InstanceCriteria.empty.showDone,
        excludedInstanceKeys: Set<InstanceKey> = InstanceCriteria.empty.excludedInstanceKeys,
        showDeleted: Boolean = TaskCriteria.empty.showDeleted,
    ) : this(
        CommonCriteria(search, showAssignedToOthers),
        TaskCriteria(showDeleted),
        InstanceCriteria(excludedInstanceKeys, showDone),
    )

    val isEmpty by lazy { this == empty }
    val isTaskEmpty by lazy { commonCriteria.isEmpty && taskCriteria.isEmpty }
    val isInstanceEmpty by lazy { commonCriteria.isEmpty && instanceCriteria.isEmpty }

    val search get() = commonCriteria.search
    val showAssignedToOthers get() = commonCriteria.showAssignedToOthers
    val showDeleted get() = taskCriteria.showDeleted
    val excludedInstanceKeys get() = instanceCriteria.excludedInstanceKeys
    val showDone get() = instanceCriteria.showDone

    fun clearSearch(): SearchCriteria {
        return if (commonCriteria.search.isEmpty) {
            this
        } else {
            copy(commonCriteria = commonCriteria.copy(search = CommonCriteria.empty.search))
        }
    }

    sealed interface Search : Parcelable {

        val hasSearch: Boolean // todo filterCriteria replace with isEmpty

        val isEmpty: Boolean

        @Parcelize
        data class Query(val query: String = "") : Search {

            companion object {

                val empty = Query()
            }

            override val isEmpty get() = this == empty

            override val hasSearch get() = query.isNotEmpty()
        }

        @Parcelize
        data class TaskKey(val taskKey: com.krystianwsul.common.utils.TaskKey) : Search {

            override val hasSearch get() = true

            override val isEmpty get() = false
        }
    }

    data class CommonCriteria(val search: Search = Search.Query.empty, val showAssignedToOthers: Boolean = true) {

        companion object {

            val empty = CommonCriteria()
        }

        val isEmpty by lazy { this == empty }
    }

    data class TaskCriteria(val showDeleted: Boolean = true) {

        companion object {

            val empty = TaskCriteria()
        }

        val isEmpty by lazy { this == empty }
    }

    data class InstanceCriteria(val excludedInstanceKeys: Set<InstanceKey> = setOf(), val showDone: Boolean = true) {

        companion object {

            val empty = InstanceCriteria()
        }

        val isEmpty by lazy { this == empty }
    }
}