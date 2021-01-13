package com.krystianwsul.treeadapter

import android.os.Parcelable
import com.krystianwsul.common.criteria.SearchCriteria
import kotlinx.parcelize.Parcelize

sealed class FilterCriteria : Parcelable {

    abstract val query: String

    val expandMatches get() = query.isNotEmpty()

    @Parcelize
    data class Full(
            override val query: String = "",
            val filterParams: FilterParams = FilterParams(),
    ) : FilterCriteria() {

        constructor(
                query: String = "",
                showDeleted: Boolean = false,
                showAssignedToOthers: Boolean = true,
                showProjects: Boolean = false,
        ) : this(query, FilterParams(showDeleted, showAssignedToOthers, showProjects))

        @Parcelize
        data class FilterParams(
                val showDeleted: Boolean = false,
                val showAssignedToOthers: Boolean = true,
                val showProjects: Boolean = false,
        ) : Parcelable
    }

    @Parcelize
    data class ExpandOnly(override val query: String = "") : FilterCriteria() {

        constructor(searchCriteria: SearchCriteria) : this(searchCriteria.query)
    }

    @Parcelize
    object None : FilterCriteria() {

        override val query get() = ""
    }
}