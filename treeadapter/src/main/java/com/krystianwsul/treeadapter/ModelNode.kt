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

    fun filter() = true

    val state: ModelState

    val id: Any

    val toggleDescendants get() = false

    val deselectParent get() = false

    fun ordinalDesc(): String? = null

    val ignoreStale: Boolean get() = false // this is a workaround for not diffing the nodes when a remote change comes in
}
