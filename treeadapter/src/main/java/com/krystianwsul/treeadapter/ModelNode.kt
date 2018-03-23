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
}
