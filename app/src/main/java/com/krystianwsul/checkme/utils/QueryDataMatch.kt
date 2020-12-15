package com.krystianwsul.checkme.utils

import com.krystianwsul.common.utils.QueryData
import com.krystianwsul.common.utils.QueryMatch

interface QueryDataMatch : QueryMatch {

    val isAssignedToMe: Boolean

    fun matchesQueryData(queryData: QueryData): Boolean {
        return (queryData.showAssigned || isAssignedToMe) && matchesQuery(queryData.query)
    }
}