package com.krystianwsul.checkme.gui.widgets

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.core.Observable

fun Observable<SearchToolbar.SearchParams>.toQuery() = map { SearchCriteria.Search.Query(it.query) }

fun Observable<SearchToolbar.SearchParams>.toSearchCriteria(showDone: Boolean, excludedInstanceKeys: Set<InstanceKey>) =
    map {
        SearchCriteria(
            SearchCriteria.Search.Query(it.query),
            it.showAssignedToOthers,
            showDone,
            excludedInstanceKeys,
        )
    }