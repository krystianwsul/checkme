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

    val expandOnMatch get() = true

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean = false)

    fun forceSelected(viewHolder: RecyclerView.ViewHolder): Unit =
        throw UnsupportedOperationException()

    fun onPayload(viewHolder: RecyclerView.ViewHolder, payloadSeparator: TreeNode.PayloadSeparator): Unit =
        throw UnsupportedOperationException()

    fun onClick(holder: T) = Unit

    fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean = false

    fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean): Boolean = true

    enum class MatchResult {

        ALWAYS_VISIBLE, MATCHES, DOESNT_MATCH;

        companion object {

            fun fromBoolean(matches: Boolean) = if (matches) MATCHES else DOESNT_MATCH
        }
    }
}
