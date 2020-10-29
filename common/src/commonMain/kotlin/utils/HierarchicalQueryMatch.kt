package com.krystianwsul.common.utils

interface HierarchicalQueryMatch : QueryMatch {

    override fun matchesQuery(query: String?): Boolean {
        if (super.matchesQuery(query)) return true

        return matchesChildren(query)
    }

    fun matchesChildren(query: String?): Boolean
}