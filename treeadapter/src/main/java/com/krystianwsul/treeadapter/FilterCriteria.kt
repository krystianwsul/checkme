package com.krystianwsul.treeadapter

import android.os.Parcelable
import com.krystianwsul.common.criteria.SearchCriteria
import kotlinx.parcelize.Parcelize

sealed class FilterCriteria : Parcelable {

    abstract val query: String

    val expandMatches get() = query.isNotEmpty()

    open fun canBeShown(treeNode: TreeNode<*>): Boolean = true

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

        override fun canBeShown(treeNode: TreeNode<*>): Boolean {
            if (!treeNode.modelNode.matchesFilterParams(filterParams)) return false

            return when (treeNode.modelNode.getMatchResult(query)) {
                ModelNode.MatchResult.ALWAYS_VISIBLE, ModelNode.MatchResult.MATCHES -> true
                ModelNode.MatchResult.DOESNT_MATCH -> treeNode.parentHierarchyMatchesQuery(query) ||
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
    data class ExpandOnly(override val query: String) : FilterCriteria() {

        constructor(searchCriteria: SearchCriteria) : this(searchCriteria.query)
    }

    @Parcelize
    object None : FilterCriteria() {

        override val query get() = ""
    }
}