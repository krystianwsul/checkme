package com.krystianwsul.treeadapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

interface TreeModelAdapter {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    val hasActionMode: Boolean

    fun incrementSelected(x: TreeViewAdapter.Placeholder)

    fun decrementSelected(x: TreeViewAdapter.Placeholder)
}
