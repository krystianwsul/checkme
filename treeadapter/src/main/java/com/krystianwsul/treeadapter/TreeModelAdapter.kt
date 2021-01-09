package com.krystianwsul.treeadapter

import android.view.ViewGroup

interface TreeModelAdapter<T : TreeHolder> : ActionModeCallback {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

    fun scrollToTop() = Unit

    fun onViewAttachedToWindow(holder: T)

    fun onViewDetachedFromWindow(holder: T)
}
