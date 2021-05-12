package com.krystianwsul.checkme.gui.tree.delegates.multiline

import com.krystianwsul.treeadapter.TreeNode

interface MultiLineModelNode {

    val rowsDelegate: RowsDelegate

    val widthKey: MultiLineDelegate.WidthKey

    val treeNode: TreeNode<*>

    interface RowsDelegate {

        fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow>
    }
}