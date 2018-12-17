package com.krystianwsul.treeadapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface TreeModelAdapter {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    val hasActionMode: Boolean

    fun incrementSelected(x: TreeViewAdapter.Placeholder)

    fun decrementSelected(x: TreeViewAdapter.Placeholder)
}
