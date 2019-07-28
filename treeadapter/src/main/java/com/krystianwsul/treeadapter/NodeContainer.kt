package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView

interface NodeContainer<T : RecyclerView.ViewHolder> {

    val isExpanded: Boolean

    val selectedChildren: List<TreeNode<T>>

    val treeNodeCollection: TreeNodeCollection<T>

    val indentation: Int

    val displayedSize: Int

    fun getPosition(treeNode: TreeNode<T>): Int

    fun remove(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder)

    fun add(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder)

    val id: Any
}
