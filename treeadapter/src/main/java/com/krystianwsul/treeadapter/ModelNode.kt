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

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun onClick(holder: T) = Unit

    fun normalize() = Unit

    fun filter(filterCriteria: Any?) = true

    fun ordinalDesc(): String? = null
}
