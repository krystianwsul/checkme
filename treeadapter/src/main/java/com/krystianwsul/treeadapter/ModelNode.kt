package com.krystianwsul.treeadapter

import android.support.v7.widget.RecyclerView

interface ModelNode : Comparable<ModelNode> {

    val itemViewType: Int

    val isSelectable get() = false

    val isVisibleWhenEmpty get() = true

    val isVisibleDuringActionMode get() = true

    val isSeparatorVisibleWhenNotExpanded get() = false

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder)

    fun onClick() = Unit

    fun getOrdinal(): Double = throw UnsupportedOperationException()

    fun setOrdinal(ordinal: Double): Unit = throw UnsupportedOperationException()

    fun matchesSearch(query: String): Boolean = true

    val state: ModelState

    val id: Any
}
