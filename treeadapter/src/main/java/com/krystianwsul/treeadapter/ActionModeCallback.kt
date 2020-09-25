package com.krystianwsul.treeadapter

interface ActionModeCallback {

    val hasActionMode: Boolean

    fun incrementSelected(placeholder: TreeViewAdapter.Placeholder)

    fun decrementSelected(placeholder: TreeViewAdapter.Placeholder)
}