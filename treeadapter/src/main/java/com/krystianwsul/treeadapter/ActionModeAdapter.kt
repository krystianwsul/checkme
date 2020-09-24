package com.krystianwsul.treeadapter

interface ActionModeAdapter {

    val hasActionMode: Boolean

    fun incrementSelected(x: TreeViewAdapter.Placeholder)

    fun decrementSelected(x: TreeViewAdapter.Placeholder)
}