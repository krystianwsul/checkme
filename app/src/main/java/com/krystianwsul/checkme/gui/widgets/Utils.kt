package com.krystianwsul.checkme.gui.widgets

import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable

fun Observable<SearchToolbar.SearchParams>.toQuery() = map { SearchCriteria.Search.Query(it.query) }