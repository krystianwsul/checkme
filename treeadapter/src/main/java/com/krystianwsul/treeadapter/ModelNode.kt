package com.krystianwsul.treeadapter

import android.support.v7.widget.RecyclerView

interface ModelNode : Comparable<ModelNode> {

    val itemViewType: Int

    val isSelectable: Boolean

    val isVisibleWhenEmpty: Boolean

    val isVisibleDuringActionMode: Boolean

    val isSeparatorVisibleWhenNotExpanded: Boolean

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder)

    fun onClick()

    fun getOrdinal(): Double = throw UnsupportedOperationException()

    fun setOrdinal(ordinal: Double): Unit = throw UnsupportedOperationException()

    fun matchesSearch(query: String): Boolean = true

    val state: ModelState

    val id: Any get() = throw java.lang.UnsupportedOperationException()
}
