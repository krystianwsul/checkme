package com.krystianwsul.common.criteria

data class SearchCriteria(val query: String = "", val showAssignedToOthers: Boolean = true) {

    val isEmpty by lazy { this == SearchCriteria() }
}