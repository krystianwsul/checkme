package com.krystianwsul.checkme.gui.tree

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.TaskKey

interface QueryMatchable { // todo optimization delete

    val normalizedFields: List<String>

    val matchesSearch: Boolean

    fun matchesTaskKey(taskKey: TaskKey): Boolean = throw UnsupportedOperationException()

    fun matchesSearch(search: SearchCriteria.Search?) = matchesSearch
}