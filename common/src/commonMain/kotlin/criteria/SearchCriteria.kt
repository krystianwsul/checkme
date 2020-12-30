package com.krystianwsul.common.criteria

data class SearchCriteria(
        val query: String = "",
        val showAssignedToOthers: Boolean = true,
        val showDone: Boolean = true,
) {

    companion object {

        val empty = SearchCriteria()
    }

    val isEmpty by lazy { this == empty }
}