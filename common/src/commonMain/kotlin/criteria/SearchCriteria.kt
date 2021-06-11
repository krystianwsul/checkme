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

    @Parcelize
    data class Search(val query: String = "") : Parcelable {

        val hasSearch get() = query.isNotEmpty()
        val expandMatches get() = hasSearch
    }
}