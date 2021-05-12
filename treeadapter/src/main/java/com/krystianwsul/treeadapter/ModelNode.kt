package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface ModelNode<T : TreeHolder> : Comparable<ModelNode<T>> {

    val itemViewType: Int

    val isSelectable get() = false

    val isSeparatorVisibleWhenNotExpanded get() = false

    val showSeparatorWhenParentExpanded get() = true

    val state: ModelState

    val id: Any

    val toggleDescendants get() = false

    val deselectParent get() = false

    val parentNode: ModelNode<T>?

    val isDraggable get() = false

    val expandOnMatch get() = true

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun onPayload(viewHolder: RecyclerView.ViewHolder, payloadSeparator: TreeNode.PayloadSeparator): Unit =
            throw UnsupportedOperationException()

    fun onClick(holder: T) = Unit

    fun normalize() = Unit

    fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) = true

    fun getMatchResult(query: String) = MatchResult.ALWAYS_VISIBLE

    fun ordinalDesc(): String? = null

    fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean = throw UnsupportedOperationException()

    fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean): Boolean = true

    enum class MatchResult {

        ALWAYS_VISIBLE, MATCHES, DOESNT_MATCH;

        companion object {

            fun fromBoolean(matches: Boolean) = if (matches) MATCHES else DOESNT_MATCH
        }
    }
}
