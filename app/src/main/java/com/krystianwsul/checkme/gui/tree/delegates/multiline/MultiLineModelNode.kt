package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val name: MultiLineRow

    val details: MultiLineRow.Visible? get() = null

    val children: MultiLineRow.Visible? get() = null

    val widthKey: MultiLineDelegate.WidthKey
}