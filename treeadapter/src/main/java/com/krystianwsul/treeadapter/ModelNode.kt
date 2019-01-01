package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface ModelNode : Comparable<ModelNode> {

    val itemViewType: Int

    val isSelectable get() = false

    val isVisibleWhenEmpty get() = true

    val isVisibleDuringActionMode get() = true

    val isSeparatorVisibleWhenNotExpanded get() = false

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun onClick() = Unit

    fun matchesSearch(query: String) = true

    val state: ModelState

    val id: Any

    val toggleDescendants get() = false

    val deselectParent get() = false
}
