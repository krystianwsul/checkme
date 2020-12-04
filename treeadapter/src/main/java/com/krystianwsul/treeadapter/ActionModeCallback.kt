package com.krystianwsul.treeadapter

interface ActionModeCallback {

    val hasActionMode: Boolean

    fun incrementSelected(placeholder: TreeViewAdapter.Placeholder, initial: Boolean = false)

    fun decrementSelected(placeholder: TreeViewAdapter.Placeholder)
}