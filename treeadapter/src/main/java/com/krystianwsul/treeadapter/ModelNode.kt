package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface ModelNode<T : RecyclerView.ViewHolder> : Comparable<ModelNode<T>> {

    val itemViewType: Int

    val isSelectable get() = false

    val isVisibleWhenEmpty get() = true

    val isVisibleDuringActionMode get() = true

    val isSeparatorVisibleWhenNotExpanded get() = false

    val showSeparatorWhenParentExpanded get() = true

    val state: ModelState

    val id: Any

    val toggleDescendants get() = false

    val deselectParent get() = false

    val parentNode: ModelNode<T>?

    val isDraggable get() = false

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun onClick(holder: T) = Unit

    fun normalize() = Unit

    fun matchesFilterParams(filterParams: TreeViewAdapter.FilterCriteria.FilterParams) = true

    fun matchesQuery(query: String) = MatchResult.ALWAYS_VISIBLE

    fun ordinalDesc(): String? = null

    fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean = throw UnsupportedOperationException()

    enum class MatchResult {

        ALWAYS_VISIBLE, MATCHES, DOESNT_MATCH;

        companion object {

            fun fromBoolean(matches: Boolean) = if (matches) MATCHES else DOESNT_MATCH
        }
    }
}
