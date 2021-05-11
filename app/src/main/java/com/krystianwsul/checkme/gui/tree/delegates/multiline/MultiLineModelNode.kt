package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val name: MultiLineRow

    val details: MultiLineRow.Visible? get() = null

    val children: MultiLineRow.Visible? get() = null

    val widthKey: MultiLineDelegate.WidthKey

    val rows get() = listOfNotNull(name, details, children).take(MultiLineDelegate.TOTAL_LINES)
}