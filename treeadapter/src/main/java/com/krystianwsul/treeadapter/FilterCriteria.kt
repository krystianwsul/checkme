package com.krystianwsul.treeadapter

import android.os.Parcelable
import com.krystianwsul.common.criteria.SearchCriteria
import kotlinx.parcelize.Parcelize

sealed interface FilterCriteria : Parcelable {

    val search: SearchCriteria.Search?

    val hasSearch get() = search?.hasSearch ?: false

    // todo optimize replace with new class where actually used for something
    @Parcelize
    data class Full(override val search: SearchCriteria.Search.Query, val filterParams: FilterParams) : FilterCriteria {

        constructor(query: String = "", filterParams: FilterParams = FilterParams()) :
                this(SearchCriteria.Search.Query(query), filterParams)

        constructor(query: String, showAssignedToOthers: Boolean) : this(query, FilterParams(showAssignedToOthers))

        fun toExpandOnly() = ExpandOnly(search) as AllowedFilterCriteria // todo optimization

        @Parcelize
        data class FilterParams(val showAssignedToOthers: Boolean = true) : Parcelable
    }

    sealed interface AllowedFilterCriteria : FilterCriteria

    @Parcelize
    data class ExpandOnly(override val search: SearchCriteria.Search?) : AllowedFilterCriteria {

        constructor(searchCriteria: SearchCriteria) : this(searchCriteria.search)
    }

    @Parcelize
    object None : AllowedFilterCriteria {

        override val search: SearchCriteria.Search? get() = null
    }
}