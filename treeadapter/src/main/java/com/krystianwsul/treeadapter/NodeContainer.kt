package com.krystianwsul.treeadapter

sealed interface NodeContainer<T : TreeHolder> {

    val isExpanded: Boolean

    val selectedChildren: List<TreeNode<T>>

    val treeNodeCollection: TreeNodeCollection<T>

    val indentation: Int

    val displayedNodes: List<TreeNode<T>>
    val displayedChildNodes: List<TreeNode<T>>
    val displayableNodes: List<TreeNode<T>>

    val displayedDirectChildNodes: List<TreeNode<T>>

    val id: Any

    fun wantsSeparators(top: Boolean): Boolean

    fun getPosition(treeNode: TreeNode<T>, positionMode: PositionMode = PositionMode.COMPAT) =
        getPosition(positionMode) { it == treeNode }

    fun getPosition(positionMode: PositionMode, matcher: (TreeNode<T>) -> Boolean): Int

    fun getNode(position: Int, positionMode: PositionMode = PositionMode.COMPAT): TreeNode<T>

    fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)

    fun add(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)

    fun swapNodePositions(fromTreeNode: TreeNode<T>, toTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)

    fun removeForSwap(fromTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)
    fun addForSwap(fromTreeNode: TreeNode<T>, toTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder)
}
