package com.krystianwsul.treeadapter

import android.view.ViewGroup

interface TreeModelAdapter<T : TreeHolder> : ActionModeCallback {

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

    fun scrollToTop() = Unit

    fun mutateIds(oldIds: List<Any>, newIds: List<Any>): Pair<List<Any>, List<Any>> = Pair(oldIds, newIds)
}
