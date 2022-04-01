package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface ModelNode<T : TreeHolder> : Comparable<ModelNode<T>>, Matchable {

    val itemViewType: Int

    val isSelectable get() = false

    val isSeparatorVisibleWhenNotExpanded get() = false

    val showSeparatorWhenParentExpanded get() = true

    val state: ModelState

    val id: Any

    val propagateSelection get() = false

    val parentNode: ModelNode<T>?

    val inheritParentBottomSeparator get() = false

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun forceSelected(viewHolder: RecyclerView.ViewHolder): Unit =
        throw UnsupportedOperationException()

    fun onPayload(viewHolder: RecyclerView.ViewHolder): Unit = throw UnsupportedOperationException()

    fun onClick(holder: T) = Unit

    fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean = false

    fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean): Boolean = true
}
