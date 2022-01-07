package com.krystianwsul.common.relevance

import com.krystianwsul.common.utils.CustomTimeKey

class CustomTimeRelevanceCollection(
        val userCustomTimeRelevances: Map<CustomTimeKey.User, CustomTimeRelevance>,
        val projectCustomTimeRelevances: Map<CustomTimeKey.Project<*>, CustomTimeRelevance>,
) {

    val relevances = userCustomTimeRelevances + projectCustomTimeRelevances

    fun getRelevance(customTimeKey: CustomTimeKey) =
        userCustomTimeRelevances[customTimeKey]
            ?: projectCustomTimeRelevances.getValue(customTimeKey as CustomTimeKey.Project<*>)
}