package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val rowsDelegate: RowsDelegate

    val widthKey: MultiLineDelegate.WidthKey

    interface RowsDelegate {

        val name: MultiLineRow
        val details: MultiLineRow.Visible? get() = null
        val children: MultiLineRow? get() = null
        val project: MultiLineRow.Visible? get() = null

        val rows get() = listOfNotNull(name, details, children, project)
    }
}