package com.krystianwsul.common.criteria

import com.krystianwsul.common.utils.InstanceKey

data class SearchCriteria(
        val query: String = "",
        val showAssignedToOthers: Boolean = true,
        val showDone: Boolean = true,
        val excludedInstanceKeys: Set<InstanceKey> = setOf(),
) {

    companion object {

        val empty = SearchCriteria()
    }

    val isEmpty by lazy { this == empty }
}