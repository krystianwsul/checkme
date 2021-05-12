package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val name: MultiLineRow

    val details: MultiLineRow.Visible? get() = null

    val children: MultiLineRow? get() = null

    val project: MultiLineRow.Visible? get() = null

    val widthKey: MultiLineDelegate.WidthKey

    val rows get() = listOfNotNull(name, details, children, project).take(MultiLineDelegate.TOTAL_LINES)

    val projectShown get() = project?.let { rows.contains(it) } ?: false
}