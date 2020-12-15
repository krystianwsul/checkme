package com.krystianwsul.checkme.utils

import com.krystianwsul.treeadapter.TreeViewAdapter

interface FilterParamsMatchable {

    val isAssignedToMe: Boolean
    val isDeleted: Boolean? get() = null

    fun matchesFilterParams(filterParams: TreeViewAdapter.FilterCriteria.FilterParams): Boolean {
        isDeleted?.let {
            if (!filterParams.showDeleted && it) return false
        }

        if (!filterParams.showAssignedToOthers && !isAssignedToMe) return false

        return true
    }
}