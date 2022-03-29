package com.krystianwsul.checkme.utils

import com.krystianwsul.treeadapter.FilterCriteria

interface FilterParamsMatchable {

    val isDeleted: Boolean? get() = null

    fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams): Boolean {
        isDeleted?.let {
            if (!filterParams.showDeleted && it) return false
        }

        return true
    }
}