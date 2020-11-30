package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface ModelNode<T : RecyclerView.ViewHolder> : Comparable<ModelNode<T>> {

    val itemViewType: Int

    val isSelectable get() = false

    val isVisibleWhenEmpty get() = true

    val isVisibleDuringActionMode get() = true

    val isSeparatorVisibleWhenNotExpanded get() = false

    val state: ModelState

    val id: Any

    val toggleDescendants get() = false

    val deselectParent get() = false

    val parentNode: ModelNode<T>?

    val isDraggable get() = false

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun onClick(holder: T) = Unit

    fun normalize() = Unit

    // does this node match the filter criteria (false if filtering not implemented, set canBeShown to true)
    fun matches(filterCriteria: Any?): Boolean

    // can the node be shown when filtering, if it doesn't match (like an ImageNode)
    // true = show
    // false = don't show
    // null = let children decide
    fun canBeShownWithFilterCriteria(filterCriteria: Any?): Boolean?

    // does this node or one of its parents match the filter criteria
    fun parentHierarchyMatches(filterCriteria: Any?): Boolean = matches(filterCriteria) || parentNode?.parentHierarchyMatches(filterCriteria) == true

    fun ordinalDesc(): String? = null

    fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean = throw UnsupportedOperationException()
}
