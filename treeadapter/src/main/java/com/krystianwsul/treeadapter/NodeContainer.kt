package com.krystianwsul.treeadapter

interface NodeContainer<T : TreeHolder> {

    val isExpanded: Boolean

    val selectedChildren: List<TreeNode<T>>

    val treeNodeCollection: TreeNodeCollection<T>

    val indentation: Int

    val displayedNodes: List<TreeNode<T>>
    val displayedChildNodes: List<TreeNode<T>>
    val displayableNodes: List<TreeNode<T>>

    val id: Any

    val wantsSeparators: Boolean

    fun getPosition(treeNode: TreeNode<T>, positionMode: PositionMode = PositionMode.COMPAT): Int

    fun getNode(position: Int, positionMode: PositionMode = PositionMode.COMPAT): TreeNode<T>

    fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)

    fun add(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)

    fun swapNodePositions(fromTreeNode: TreeNode<T>, toTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)
}
