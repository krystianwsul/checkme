package com.krystianwsul.checkme.utils

import com.krystianwsul.common.criteria.QueryData
import com.krystianwsul.common.criteria.QueryMatch

interface QueryDataMatch : QueryMatch {

    val isAssignedToMe: Boolean

    fun matchesQueryData(queryData: QueryData): Boolean {
        return (queryData.showAssigned || isAssignedToMe) && matchesQuery(queryData.query)
    }
}