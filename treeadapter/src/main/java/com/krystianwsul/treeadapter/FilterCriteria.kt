package com.krystianwsul.treeadapter

import android.os.Parcelable
import com.krystianwsul.common.criteria.SearchCriteria
import kotlinx.parcelize.Parcelize

sealed interface FilterCriteria : Parcelable {

    val search: SearchCriteria.Search?

    val hasSearch get() = search?.hasSearch ?: false
    val expandMatches get() = search?.expandMatches ?: false
    val needsNormalization get() = hasSearch

    fun canBeShown(treeNode: TreeNode<*>): Boolean = true

    @Parcelize
    data class Full(override val search: SearchCriteria.Search, val filterParams: FilterParams) : FilterCriteria {

        constructor(query: String = "", filterParams: FilterParams = FilterParams()) :
                this(SearchCriteria.Search(query), filterParams)

        constructor(
            query: String = "",
            showDeleted: Boolean = false,
            showAssignedToOthers: Boolean = true,
            showProjects: Boolean = false,
        ) : this(query, FilterParams(showDeleted, showAssignedToOthers, showProjects))

        override fun canBeShown(treeNode: TreeNode<*>): Boolean {
            if (!treeNode.modelNode.matchesFilterParams(filterParams)) return false

            return when (treeNode.modelNode.getMatchResult(search)) {
                ModelNode.MatchResult.ALWAYS_VISIBLE, ModelNode.MatchResult.MATCHES -> true
                ModelNode.MatchResult.DOESNT_MATCH -> treeNode.parentHierarchyMatchesSearch(search) ||
                        treeNode.childHierarchyMatchesFilterCriteria(this)
            }
        }

        @Parcelize
        data class FilterParams(
            val showDeleted: Boolean = false,
            val showAssignedToOthers: Boolean = true,
            val showProjects: Boolean = false,
        ) : Parcelable
    }

    @Parcelize
    data class ExpandOnly(override val search: SearchCriteria.Search?) : FilterCriteria {

        constructor(searchCriteria: SearchCriteria) : this(searchCriteria.search)
    }

    @Parcelize
    object None : FilterCriteria {

        override val search: SearchCriteria.Search? get() = null
    }
}