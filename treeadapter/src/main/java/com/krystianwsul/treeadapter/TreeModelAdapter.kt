package com.krystianwsul.treeadapter

import android.view.ViewGroup

interface TreeModelAdapter {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewAdapter.Holder

    val hasActionMode: Boolean

    fun incrementSelected(x: TreeViewAdapter.Placeholder)

    fun decrementSelected(x: TreeViewAdapter.Placeholder)

    fun scrollToTop() = Unit
}
