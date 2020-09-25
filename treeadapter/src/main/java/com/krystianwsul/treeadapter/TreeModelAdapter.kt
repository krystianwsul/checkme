package com.krystianwsul.treeadapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface TreeModelAdapter<T : RecyclerView.ViewHolder> : ActionModeCallback {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

    fun scrollToTop() = Unit

    fun onViewAttachedToWindow(holder: T)

    fun onViewDetachedFromWindow(holder: T)
}
