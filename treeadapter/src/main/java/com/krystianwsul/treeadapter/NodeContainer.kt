package com.krystianwsul.treeadapter

interface NodeContainer {

    val isExpanded: Boolean

    val selectedChildren: List<TreeNode>

    val treeNodeCollection: TreeNodeCollection

    val indentation: Int

    val displayedSize: Int

    fun getPosition(treeNode: TreeNode): Int

    fun remove(treeNode: TreeNode, x: TreeViewAdapter.Placeholder)

    fun add(treeNode: TreeNode, x: TreeViewAdapter.Placeholder)

    val id: Any
}
