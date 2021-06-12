package com.krystianwsul.treeadapter

import android.view.ViewGroup

interface TreeModelAdapter<T : TreeHolder> : ActionModeCallback {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

    fun scrollToTop()
}
