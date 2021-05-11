package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val name: MultiLineRow

    val details: Pair<String, Int>? get() = null

    val children: Pair<String, Int>? get() = null

    val widthKey: MultiLineDelegate.WidthKey
}