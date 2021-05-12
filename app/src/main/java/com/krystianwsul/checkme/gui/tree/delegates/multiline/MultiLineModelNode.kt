package com.krystianwsul.checkme.gui.tree.delegates.multiline

import com.krystianwsul.treeadapter.TreeNode

interface MultiLineModelNode {

    val rowsDelegate: RowsDelegate

    val widthKey: MultiLineDelegate.WidthKey

    val treeNode: TreeNode<*>

    interface RowsDelegate {

        val name: MultiLineRow
        val details: MultiLineRow.Visible? get() = null
        val children: MultiLineRow? get() = null
        val project: MultiLineRow.Visible? get() = null

        fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>) = listOfNotNull(name, details, children, project)
    }
}