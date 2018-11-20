package com.krystianwsul.treeadapter

interface NodeContainer {

    val isExpanded: Boolean

    val selectedChildren: List<TreeNode>

    val treeNodeCollection: TreeNodeCollection

    val indentation: Int

    val displayedSize: Int

    fun getPosition(treeNode: TreeNode): Int

    fun update()

    fun updateRecursive()

    fun remove(treeNode: TreeNode, x: Any)

    fun add(treeNode: TreeNode, x: Any)

    val id: Any
}
