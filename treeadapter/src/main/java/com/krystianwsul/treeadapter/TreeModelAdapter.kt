package com.krystianwsul.treeadapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface TreeModelAdapter<T : RecyclerView.ViewHolder> {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

    val hasActionMode: Boolean

    fun incrementSelected(x: TreeViewAdapter.Placeholder)

    fun decrementSelected(x: TreeViewAdapter.Placeholder)

    fun scrollToTop() = Unit

    fun onViewAttachedToWindow(holder: T)

    fun onViewDetachedFromWindow(holder: T)
}
